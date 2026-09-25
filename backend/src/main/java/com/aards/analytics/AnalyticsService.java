package com.aards.analytics;

import com.aards.analytics.dto.AnalyticsFilterRequest;
import com.aards.analytics.dto.BacklogDistributionDto;
import com.aards.analytics.dto.CardsDto;
import com.aards.analytics.dto.DashboardResponse;
import com.aards.analytics.dto.FilterOptionsDto;
import com.aards.analytics.dto.GradeDistributionDto;
import com.aards.analytics.dto.SubjectPerformanceDto;
import com.aards.department.Department;
import com.aards.department.DepartmentRepository;
import com.aards.result.ResultRepository;
import com.aards.result.ResultStatus;
import com.aards.semesterresult.SemesterResult;
import com.aards.semesterresult.SemesterResultRepository;
import com.aards.semesterresult.SemesterStatus;
import com.aards.session.AcademicSessionRepository;
import com.aards.student.Student;
import com.aards.student.StudentRepository;
import com.aards.subject.Subject;
import com.aards.subject.SubjectRepository;
import com.aards.user.Role;
import com.aards.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Builds dashboard numbers from SemesterResult + Result tables.
// No native SQL, only simple repository calls + Java streams.
@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final SemesterResultRepository semesterResultRepository;
    private final ResultRepository resultRepository;
    private final SubjectRepository subjectRepository;
    private final StudentRepository studentRepository;
    private final AcademicSessionRepository sessionRepository;
    private final DepartmentRepository departmentRepository;

    public AnalyticsService(SemesterResultRepository semesterResultRepository,
                            ResultRepository resultRepository,
                            SubjectRepository subjectRepository,
                            StudentRepository studentRepository,
                            AcademicSessionRepository sessionRepository,
                            DepartmentRepository departmentRepository) {
        this.semesterResultRepository = semesterResultRepository;
        this.resultRepository = resultRepository;
        this.subjectRepository = subjectRepository;
        this.studentRepository = studentRepository;
        this.sessionRepository = sessionRepository;
        this.departmentRepository = departmentRepository;
    }

    public DashboardResponse getDashboard(AnalyticsFilterRequest filter, User currentUser) {
        applyRoleScope(filter, currentUser);
        log.info("Analytics generated for session={}, dept={}, year={}, sem={}",
                filter.getAcademicSessionId(), filter.getDepartmentId(),
                filter.getYear(), filter.getSemester());

        // a. All semester rows for session+year+sem, then keep only this department's students.
        log.info("Dashboard query: sessionId={}, deptId={}, year={}, semester={}",
                filter.getAcademicSessionId(), filter.getDepartmentId(),
                filter.getYear(), filter.getSemester());
        List<SemesterResult> all = semesterResultRepository
                .findByAcademicSessionIdAndYearAndSemester(
                        filter.getAcademicSessionId(), filter.getYear(), filter.getSemester());
        log.info("Found {} semester results for query", all.size());
        Set<Long> deptStudentIds = studentRepository
                .findByDepartmentId(filter.getDepartmentId())
                .stream().map(Student::getId).collect(Collectors.toSet());
        List<SemesterResult> rows = all.stream()
                .filter(r -> deptStudentIds.contains(r.getStudentId()))
                .toList();

        // b. No data yet: return zeros, not an error.
        if (rows.isEmpty()) {
            log.warn("No data found for filter: sessionId={}, deptId={}, year={}, semester={}",
                    filter.getAcademicSessionId(), filter.getDepartmentId(),
                    filter.getYear(), filter.getSemester());
            log.info("No semester results for filter, returning empty dashboard");
            return DashboardResponse.builder()
                    .cards(new CardsDto())
                    .subjectPerformance(List.of())
                    .backlogDistribution(emptyBacklogs())
                    .gradeDistribution(List.of())
                    .build();
        }

        // c-e. Headcounts.
        long total = rows.size();
        long passed = rows.stream().filter(r -> r.getStatus() == SemesterStatus.PASS).count();
        long failed = total - passed;

        // f. Pass % (guard divide by zero).
        double passPct = round2(safeDivide(passed * 100.0, total));

        // g-i. SGPA stats.
        double avg = round2(rows.stream().mapToDouble(r -> r.getSgpa() == null ? 0 : r.getSgpa()).average().orElse(0));
        double highest = round2(rows.stream().mapToDouble(r -> r.getSgpa() == null ? 0 : r.getSgpa()).max().orElse(0));
        double lowest = round2(rows.stream().mapToDouble(r -> r.getSgpa() == null ? 0 : r.getSgpa()).min().orElse(0));

        // j-l. Backlog buckets.
        long b1 = rows.stream().filter(r -> r.getBacklogCount() != null && r.getBacklogCount() == 1).count();
        long b2 = rows.stream().filter(r -> r.getBacklogCount() != null && r.getBacklogCount() == 2).count();
        long b3 = rows.stream().filter(r -> r.getBacklogCount() != null && r.getBacklogCount() >= 3).count();
        long b0 = total - b1 - b2 - b3;

        // m. Topper = highest SGPA row.
        SemesterResult topper = rows.stream()
                .max(Comparator.comparingDouble(r -> r.getSgpa() == null ? 0 : r.getSgpa()))
                .orElse(null);
        String topperName = null;
        Double topperSgpa = null;
        if (topper != null) {
            topperSgpa = topper.getSgpa();
            topperName = studentRepository.findById(topper.getStudentId())
                    .map(Student::getFullName).orElse(null);
        }

        CardsDto cards = buildCards(total, passed, failed, passPct, avg, highest, lowest, b1, b2, b3);

        // n. One row per subject with pass %.
        List<SubjectPerformanceDto> subjectPerformance = subjectRepository
                .findByDepartmentIdAndYearAndSemester(
                        filter.getDepartmentId(), filter.getYear(), filter.getSemester())
                .stream().map(subject -> toSubjectPerformance(subject, filter.getAcademicSessionId()))
                .toList();

        // o. Backlog chart data.
        List<BacklogDistributionDto> backlogs = List.of(
                BacklogDistributionDto.builder().label("0 Backlogs").count(b0).build(),
                BacklogDistributionDto.builder().label("1 Backlog").count(b1).build(),
                BacklogDistributionDto.builder().label("2 Backlogs").count(b2).build(),
                BacklogDistributionDto.builder().label("3+ Backlogs").count(b3).build());

        // p. Grade chart data.
        List<GradeDistributionDto> grades = resultRepository
                .findByAcademicSessionIdAndYearAndSemesterAndStudent_DepartmentId(
                        filter.getAcademicSessionId(), filter.getYear(),
                        filter.getSemester(), filter.getDepartmentId())
                .stream()
                .collect(Collectors.groupingBy(
                        r -> r.getGrade() == null || r.getGrade().isBlank() ? "NA" : r.getGrade(),
                        Collectors.counting()))
                .entrySet().stream()
                .map(e -> GradeDistributionDto.builder().grade(e.getKey()).count(e.getValue()).build())
                .sorted(Comparator.comparing(GradeDistributionDto::getGrade))
                .toList();

        // q. Full response.
        return DashboardResponse.builder()
                .cards(cards)
                .subjectPerformance(subjectPerformance)
                .backlogDistribution(backlogs)
                .gradeDistribution(grades)
                .topperName(topperName)
                .topperSgpa(topperSgpa)
                .build();
    }

    public FilterOptionsDto getFilterOptions(User currentUser) {
        log.info("Fetching dashboard filter options");
        List<FilterOptionsDto.SessionOption> sessions = sessionRepository.findAll().stream()
                .map(s -> FilterOptionsDto.SessionOption.builder().id(s.getId()).name(s.getName()).build())
                .toList();
        // HODs only ever see their own department. Everyone else sees all.
        List<Department> departments;
        if (currentUser != null && currentUser.getRole() == Role.HOD) {
            if (currentUser.getDepartmentId() == null) {
                throw new RuntimeException("HOD has no department assigned.");
            }
            departments = departmentRepository.findById(currentUser.getDepartmentId())
                    .map(List::of).orElse(List.of());
        } else {
            departments = departmentRepository.findAll();
        }
        List<FilterOptionsDto.DepartmentOption> options = departments.stream()
                .map(d -> FilterOptionsDto.DepartmentOption.builder()
                        .id(d.getId()).name(d.getName()).code(d.getCode()).build())
                .toList();
        return FilterOptionsDto.builder()
                .sessions(sessions)
                .departments(options)
                .years(List.of(1, 2, 3, 4))
                .semesters(List.of(1, 2, 3, 4, 5, 6, 7, 8))
                .build();
    }

    // HODs are locked to their own department. Other roles keep the filter as given.
    private void applyRoleScope(AnalyticsFilterRequest filter, User currentUser) {
        if (currentUser == null || currentUser.getRole() != Role.HOD) {
            return;
        }
        if (currentUser.getDepartmentId() == null) {
            throw new RuntimeException("HOD has no department assigned.");
        }
        if (!currentUser.getDepartmentId().equals(filter.getDepartmentId())) {
            log.warn("HOD {} asked for out-of-scope department {}, forcing {}",
                    currentUser.getUsername(), filter.getDepartmentId(),
                    currentUser.getDepartmentId());
        }
        filter.setDepartmentId(currentUser.getDepartmentId());
    }

    private SubjectPerformanceDto toSubjectPerformance(Subject subject, Long sessionId) {
        long passed = resultRepository.countBySubjectIdAndAcademicSessionIdAndStatus(
                subject.getId(), sessionId, ResultStatus.PASS);
        long failed = resultRepository.countBySubjectIdAndAcademicSessionIdAndStatus(
                subject.getId(), sessionId, ResultStatus.FAIL);
        long total = passed + failed;
        return SubjectPerformanceDto.builder()
                .subjectId(subject.getId())
                .subjectCode(subject.getCode())
                .subjectName(subject.getName())
                .totalStudents(total)
                .passedStudents(passed)
                .failedStudents(failed)
                .passPercentage(round2(safeDivide(passed * 100.0, total)))
                .build();
    }

    private CardsDto buildCards(long total, long passed, long failed, double passPct,
                                double avg, double highest, double lowest,
                                long b1, long b2, long b3) {
        return CardsDto.builder()
                .totalStudents(total)
                .passedStudents(passed)
                .failedStudents(failed)
                .overallPassPercentage(passPct)
                .averageSgpa(avg)
                .highestSgpa(highest)
                .lowestSgpa(lowest)
                .studentsWith1Backlog(b1)
                .studentsWith2Backlogs(b2)
                .studentsWith3PlusBacklogs(b3)
                .build();
    }

    private List<BacklogDistributionDto> emptyBacklogs() {
        return List.of(
                BacklogDistributionDto.builder().label("0 Backlogs").count(0).build(),
                BacklogDistributionDto.builder().label("1 Backlog").count(0).build(),
                BacklogDistributionDto.builder().label("2 Backlogs").count(0).build(),
                BacklogDistributionDto.builder().label("3+ Backlogs").count(0).build());
    }

    private double safeDivide(double numerator, double denominator) {
        if (denominator == 0) {
            return 0;
        }
        return numerator / denominator;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
