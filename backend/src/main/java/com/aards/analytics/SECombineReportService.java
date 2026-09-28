package com.aards.analytics;

import com.aards.analytics.dto.AnalyticsFilterRequest;
import com.aards.department.DepartmentRepository;
import com.aards.report.dto.SECombineReportResponse;
import com.aards.result.Result;
import com.aards.result.ResultRepository;
import com.aards.result.ResultStatus;
import com.aards.semesterresult.SemesterResult;
import com.aards.semesterresult.SemesterResultRepository;
import com.aards.session.AcademicSessionRepository;
import com.aards.student.Student;
import com.aards.student.StudentRepository;
import com.aards.subject.Subject;
import com.aards.subject.SubjectFacultyRepository;
import com.aards.subject.SubjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// SE Combine numbers: distribution + backlog + overall + subject tables + toppers.
// One row per student averaged across both semesters of the year.
@Service
public class SECombineReportService {

    private static final Logger log = LoggerFactory.getLogger(SECombineReportService.class);

    private final SemesterResultRepository semesterResultRepository;
    private final ResultRepository resultRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final SubjectFacultyRepository subjectFacultyRepository;
    private final DepartmentRepository departmentRepository;
    private final AcademicSessionRepository sessionRepository;

    public SECombineReportService(SemesterResultRepository semesterResultRepository,
                                  ResultRepository resultRepository,
                                  StudentRepository studentRepository,
                                  SubjectRepository subjectRepository,
                                  SubjectFacultyRepository subjectFacultyRepository,
                                  DepartmentRepository departmentRepository,
                                  AcademicSessionRepository sessionRepository) {
        this.semesterResultRepository = semesterResultRepository;
        this.resultRepository = resultRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.subjectFacultyRepository = subjectFacultyRepository;
        this.departmentRepository = departmentRepository;
        this.sessionRepository = sessionRepository;
    }

    @Transactional(readOnly = true)
    public SECombineReportResponse generateSECombineReport(AnalyticsFilterRequest filter) {
        log.info("SE Combine Report generated: session={}, dept={}, year={}",
                filter.getAcademicSessionId(), filter.getDepartmentId(), filter.getYear());

        int semA = filter.getYear() * 2 - 1;
        int semB = filter.getYear() * 2;

        // a. Semester rows for both semesters, grouped per student.
        Map<Long, Double> sgpaA = new HashMap<>();
        Map<Long, Double> sgpaB = new HashMap<>();
        for (SemesterResult r : semesterResultRepository
                .findByAcademicSessionIdAndYearAndSemester(
                        filter.getAcademicSessionId(), filter.getYear(), semA)) {
            sgpaA.put(r.getStudentId(), r.getSgpa());
        }
        for (SemesterResult r : semesterResultRepository
                .findByAcademicSessionIdAndYearAndSemester(
                        filter.getAcademicSessionId(), filter.getYear(), semB)) {
            sgpaB.put(r.getStudentId(), r.getSgpa());
        }

        // b. All results of this department for the year, grouped per student.
        List<Result> allResults = resultRepository
                .findByAcademicSessionIdAndYearAndStudent_DepartmentId(
                        filter.getAcademicSessionId(), filter.getYear(), filter.getDepartmentId());
        Map<Long, List<Result>> resultsByStudent = allResults.stream()
                .collect(Collectors.groupingBy(r -> r.getStudent().getId()));

        // c. One summary per student.
        Set<Long> studentIds = new HashSet<>();
        studentIds.addAll(sgpaA.keySet());
        studentIds.addAll(sgpaB.keySet());
        studentIds.addAll(resultsByStudent.keySet());
        Map<Long, Student> students = studentRepository.findAllById(studentIds).stream()
                .collect(Collectors.toMap(Student::getId, s -> s));

        List<StudentSummary> summaries = new ArrayList<>();
        for (Long studentId : studentIds) {
            Student student = students.get(studentId);
            if (student == null) {
                continue;
            }
            List<Double> sgpas = new ArrayList<>();
            if (sgpaA.get(studentId) != null) {
                sgpas.add(sgpaA.get(studentId));
            }
            if (sgpaB.get(studentId) != null) {
                sgpas.add(sgpaB.get(studentId));
            }
            // Both semesters null (ATKT ledger shows "----"): fall back to the
            // latest prior-year SGPA so the student stays in distribution.
            // Null survives only when no prior SGPA exists (unclassified).
            Double avgSgpa;
            if (sgpas.isEmpty()) {
                avgSgpa = fallbackSgpa(studentId, filter.getAcademicSessionId(), filter.getYear());
            } else {
                avgSgpa = sgpas.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            }
            List<Result> rows = resultsByStudent.getOrDefault(studentId, List.of());
            long backlogCount = rows.stream().filter(r -> r.getStatus() == ResultStatus.FAIL).count();
            long passedSubjectCount = rows.stream().filter(r -> r.getStatus() == ResultStatus.PASS).count();
            double totalMarks = rows.stream()
                    .mapToDouble(r -> r.getMarksObtained() == null ? 0 : r.getMarksObtained()).sum();
            summaries.add(new StudentSummary(student, avgSgpa, backlogCount, passedSubjectCount, totalMarks));
        }

        long total = summaries.size();
        List<StudentSummary> withAvg = summaries.stream()
                .filter(s -> s.avgSgpa != null).toList();
        // Distribution covers all-clear students only (ATKT students live in
        // the backlog table). Bands must add up to All Clear.
        List<StudentSummary> clearWithAvg = withAvg.stream()
                .filter(s -> s.backlogCount == 0).toList();

        // d. Distribution bands on avg SGPA. Null averages (no SGPA anywhere)
        // go to the unclassified bucket instead of vanishing from totals.
        long distinction = countInBand(clearWithAvg, 7.75, Double.MAX_VALUE);
        long firstClass = countInBand(clearWithAvg, 6.75, 7.75);
        long higherSecond = countInBand(clearWithAvg, 6.25, 6.75);
        long secondClass = countInBand(clearWithAvg, 5.5, 6.25);
        long passClass = clearWithAvg.stream().filter(s -> s.avgSgpa < 5.5).count();
        long unclassified = summaries.stream().filter(s -> s.avgSgpa == null).count();

        // e. Backlog buckets.
        long failedInOne = countBacklogs(summaries, 1);
        long failedInTwo = countBacklogs(summaries, 2);
        long failedInThree = countBacklogs(summaries, 3);
        long failedInFour = countBacklogs(summaries, 4);
        long failedInFiveOrMore = summaries.stream().filter(s -> s.backlogCount >= 5).count();

        // f. Overall summary. Quality is a subset of All Clear; fail means
        // zero passed subjects; ATKT sits in between.
        long allClear = summaries.stream().filter(s -> s.backlogCount == 0).count();
        long quality = summaries.stream()
                .filter(s -> s.backlogCount == 0 && s.avgSgpa != null && s.avgSgpa >= 6.75).count();
        long withAtkt = summaries.stream()
                .filter(s -> s.backlogCount >= 1 && s.passedSubjectCount >= 1).count();
        long fail = summaries.stream().filter(s -> s.passedSubjectCount == 0).count();
        long absent = allResults.stream().filter(r -> r.getStatus() == ResultStatus.ABSENT).count();

        // g. Subject tables, Semester II first to match the Excel sheet.
        List<SECombineReportResponse.SemesterBlock> semesters = List.of(
                buildSemesterBlock(filter, allResults, semB, 2, "Semester II"),
                buildSemesterBlock(filter, allResults, semA, 1, "Semester I"));

        // h. Top 5 by avg SGPA, ties broken by total marks.
        List<SECombineReportResponse.TopperRow> toppers = withAvg.stream()
                .sorted(Comparator.comparingDouble((StudentSummary s) -> s.avgSgpa).reversed()
                        .thenComparing(Comparator.comparingDouble((StudentSummary s) -> s.totalMarks).reversed()))
                .limit(5)
                .map(s -> SECombineReportResponse.TopperRow.builder()
                        .rank(0)
                        .name(s.student.getFullName())
                        .sgpa(round2(s.avgSgpa))
                        .build())
                .toList();
        for (int i = 0; i < toppers.size(); i++) {
            toppers.get(i).setRank(i + 1);
        }

        String deptName = departmentRepository.findById(filter.getDepartmentId())
                .map(d -> d.getCode() + " - " + d.getName())
                .orElse("Department " + filter.getDepartmentId());
        String sessionName = sessionRepository.findById(filter.getAcademicSessionId())
                .map(s -> s.getName()).orElse("Session " + filter.getAcademicSessionId());

        return SECombineReportResponse.builder()
                .header(SECombineReportResponse.Header.builder()
                        .collegeName("AARDS College of Engineering")
                        .departmentName(deptName)
                        .sessionName(sessionName)
                        .yearLabel(yearLabel(filter.getYear()))
                        .semesterLabel("Sem " + semA + " & Sem " + semB)
                        .generatedOn(LocalDate.now().toString())
                        .totalAppearedHeader(total)
                        .build())
                .distribution(SECombineReportResponse.Distribution.builder()
                        .distinction(countPct(distinction, total))
                        .firstClass(countPct(firstClass, total))
                        .higherSecond(countPct(higherSecond, total))
                        .secondClass(countPct(secondClass, total))
                        .passClass(countPct(passClass, total))
                        .unclassified(countPct(unclassified, total))
                        .build())
                .backlog(SECombineReportResponse.Backlog.builder()
                        .failedInOne(failedInOne)
                        .failedInTwo(failedInTwo)
                        .failedInThree(failedInThree)
                        .failedInFour(failedInFour)
                        .failedInFiveOrMore(failedInFiveOrMore)
                        .build())
                .overall(SECombineReportResponse.Overall.builder()
                        .totalAppeared(total)
                        .allClear(allClear)
                        .allClearPct(pct(allClear, total))
                        .quality(quality)
                        .qualityPct(pct(quality, total))
                        .withAtkt(withAtkt)
                        .withAtktPct(pct(withAtkt, total))
                        .fail(fail)
                        .failPct(pct(fail, total))
                        .absent(absent)
                        .build())
                .semesters(semesters)
                .toppers(toppers)
                .build();
    }

    // One subject table: rows grouped by subject, ordered by code.
    private SECombineReportResponse.SemesterBlock buildSemesterBlock(
            AnalyticsFilterRequest filter, List<Result> allResults, int semester,
            int displayNumber, String displayName) {
        Map<Long, List<Result>> bySubject = allResults.stream()
                .filter(r -> r.getSemester() != null && r.getSemester() == semester)
                .collect(Collectors.groupingBy(r -> r.getSubject().getId()));
        Map<Long, Subject> subjects = subjectRepository.findAllById(bySubject.keySet()).stream()
                .collect(Collectors.toMap(Subject::getId, s -> s));
        List<Subject> ordered = subjects.values().stream()
                .sorted(Comparator.comparing(Subject::getCode,
                        Comparator.nullsLast(String::compareTo)))
                .toList();

        List<SECombineReportResponse.SubjectRow> rows = new ArrayList<>();
        int sn = 0;
        for (Subject subject : ordered) {
            List<Result> subjectRows = bySubject.getOrDefault(subject.getId(), List.of());
            Set<Long> onRoll = new HashSet<>();
            Set<Long> appeared = new HashSet<>();
            Set<Long> passed = new HashSet<>();
            long dist = 0, first = 0, hsc = 0, sc = 0, pass = 0;
            Double highest = null;
            for (Result r : subjectRows) {
                onRoll.add(r.getStudent().getId());
                if (r.getStatus() == ResultStatus.ABSENT) {
                    continue;
                }
                appeared.add(r.getStudent().getId());
                if (r.getStatus() == ResultStatus.PASS) {
                    passed.add(r.getStudent().getId());
                }
                double marks = r.getMarksObtained() == null ? 0 : r.getMarksObtained();
                if (marks >= 65) {
                    dist++;
                } else if (marks >= 60) {
                    first++;
                } else if (marks >= 55) {
                    hsc++;
                } else if (marks >= 50) {
                    sc++;
                } else if (marks >= 40) {
                    pass++;
                }
                if (highest == null || marks > highest) {
                    highest = marks;
                }
            }
            String facultyName = subjectFacultyRepository
                    .findBySubjectIdAndAcademicSessionId(subject.getId(), filter.getAcademicSessionId())
                    .map(sf -> sf.getFaculty().getFullName()).orElse("—");
            rows.add(SECombineReportResponse.SubjectRow.builder()
                    .sn(++sn)
                    .subjectCode(subject.getCode())
                    .subjectName(subject.getName())
                    .facultyName(facultyName)
                    .onRoll(onRoll.size())
                    .appeared(appeared.size())
                    .passed(passed.size())
                    .passingPercentage(pct(passed.size(), appeared.size()))
                    .distinction(dist)
                    .firstClass(first)
                    .higherSecond(hsc)
                    .secondClass(sc)
                    .passClass(pass)
                    .highestMarks(highest)
                    .build());
        }
        return SECombineReportResponse.SemesterBlock.builder()
                .semesterNumber(displayNumber)
                .semesterDisplayName(displayName)
                .subjects(rows)
                .build();
    }

    private long countInBand(List<StudentSummary> summaries, double from, double to) {
        return summaries.stream()
                .filter(s -> s.avgSgpa >= from && s.avgSgpa < to).count();
    }

    private long countBacklogs(List<StudentSummary> summaries, int backlogs) {
        return summaries.stream().filter(s -> s.backlogCount == backlogs).count();
    }

    // Latest non-null SGPA from an earlier year, for ATKT students whose both
    // current semesters show "----". Null when no prior SGPA exists.
    private Double fallbackSgpa(Long studentId, Long sessionId, int year) {
        return semesterResultRepository.findByStudentIdAndAcademicSessionId(studentId, sessionId)
                .stream()
                .filter(r -> r.getYear() != null && r.getYear() < year)
                .filter(r -> r.getSgpa() != null)
                .max(Comparator.comparingInt(
                        r -> r.getYear() * 100 + (r.getSemester() == null ? 0 : r.getSemester())))
                .map(SemesterResult::getSgpa)
                .orElse(null);
    }

    private SECombineReportResponse.CountPct countPct(long count, long total) {
        return SECombineReportResponse.CountPct.builder()
                .count(count).percentage(pct(count, total)).build();
    }

    private double pct(long part, long total) {
        if (total == 0) {
            return 0;
        }
        return round2(part * 100.0 / total);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String yearLabel(int year) {
        return switch (year) {
            case 1 -> "F.E.";
            case 2 -> "S.E.";
            case 3 -> "T.E.";
            case 4 -> "B.E.";
            default -> "Year " + year;
        };
    }

    // One student averaged across both semesters.
    private static class StudentSummary {
        final Student student;
        final Double avgSgpa;
        final long backlogCount;
        final long passedSubjectCount;
        final double totalMarks;

        StudentSummary(Student student, Double avgSgpa, long backlogCount,
                       long passedSubjectCount, double totalMarks) {
            this.student = student;
            this.avgSgpa = avgSgpa;
            this.backlogCount = backlogCount;
            this.passedSubjectCount = passedSubjectCount;
            this.totalMarks = totalMarks;
        }
    }
}
