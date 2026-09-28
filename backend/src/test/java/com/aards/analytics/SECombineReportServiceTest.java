package com.aards.analytics;

import com.aards.analytics.dto.AnalyticsFilterRequest;
import com.aards.analytics.dto.SECombineReconciliationRow;
import com.aards.department.Department;
import com.aards.department.DepartmentRepository;
import com.aards.report.dto.SECombineReportResponse;
import com.aards.result.Result;
import com.aards.result.ResultRepository;
import com.aards.result.ResultStatus;
import com.aards.semesterresult.SemesterResult;
import com.aards.semesterresult.SemesterResultRepository;
import com.aards.semesterresult.SemesterStatus;
import com.aards.session.AcademicSession;
import com.aards.session.AcademicSessionRepository;
import com.aards.student.Student;
import com.aards.student.StudentRepository;
import com.aards.subject.Subject;
import com.aards.subject.SubjectFaculty;
import com.aards.subject.SubjectFacultyRepository;
import com.aards.subject.SubjectRepository;
import com.aards.user.Role;
import com.aards.user.User;
import com.aards.user.repository.UserRepository;
import com.aards.yearresult.YearResult;
import com.aards.yearresult.YearResultRepository;
import com.aards.yearresult.YearResultStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 10 students with spread-out SGPAs, backlogs and marks. Checks distribution
// bands, backlog buckets, overall maths, subject rows and topper ranking.
@SpringBootTest
@Transactional
class SECombineReportServiceTest {

    @Autowired
    private SECombineReportService seCombineReportService;

    @Autowired
    private AcademicSessionRepository sessionRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SemesterResultRepository semesterResultRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private SubjectFacultyRepository subjectFacultyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private YearResultRepository yearResultRepository;

    private Student addStudent(String prn, String name, Long deptId,
                               AcademicSession session, Double sgpa3, Double sgpa4) {
        Student student = studentRepository.save(Student.builder()
                .prn(prn)
                .rollNumber("R" + prn)
                .fullName(name)
                .departmentId(deptId)
                .currentYear(2)
                .currentSemester(4)
                .active(true)
                .build());
        if (sgpa3 != null) {
            semesterResultRepository.save(SemesterResult.builder()
                    .studentId(student.getId())
                    .academicSessionId(session.getId())
                    .year(2).semester(3).sgpa(sgpa3)
                    .backlogCount(0).status(SemesterStatus.PASS).build());
        }
        if (sgpa4 != null) {
            semesterResultRepository.save(SemesterResult.builder()
                    .studentId(student.getId())
                    .academicSessionId(session.getId())
                    .year(2).semester(4).sgpa(sgpa4)
                    .backlogCount(0).status(SemesterStatus.PASS).build());
        }
        return student;
    }

    private void addResult(Student student, Subject subject, AcademicSession session,
                           double marks, String grade, ResultStatus status) {
        resultRepository.save(Result.builder()
                .student(student).subject(subject).academicSession(session)
                .year(2).semester(3)
                .marksObtained(marks).grade(grade)
                .status(status).backlog(status == ResultStatus.FAIL).build());
    }

    private void addOfficial(Student student, AcademicSession session,
                             String raw, YearResultStatus status) {
        yearResultRepository.save(YearResult.builder()
                .studentId(student.getId()).academicSessionId(session.getId())
                .year(2).officialResultRaw(raw).status(status).build());
    }

    @Test
    void combineNumbersAreCorrect() {
        AcademicSession session = sessionRepository.save(AcademicSession.builder()
                .name("2035-36").active(true).build());
        Department dept = departmentRepository.save(Department.builder()
                .name("Combine Dept").code("CMB").build());

        Subject se201 = subjectRepository.save(Subject.builder()
                .code("SE201").name("Combine Subject One")
                .departmentId(dept.getId()).year(2).semester(3)
                .credits(3).maxMarks(100).passingMarks(40).build());
        Subject se202 = subjectRepository.save(Subject.builder()
                .code("SE202").name("Combine Subject Two")
                .departmentId(dept.getId()).year(2).semester(3)
                .credits(3).maxMarks(100).passingMarks(40).build());
        Subject se203 = subjectRepository.save(Subject.builder()
                .code("SE203").name("Combine Subject Three")
                .departmentId(dept.getId()).year(2).semester(3)
                .credits(3).maxMarks(100).passingMarks(40).build());
        User faculty = userRepository.save(User.builder()
                .username("combine_faculty").password("test")
                .fullName("Combine Faculty").email("combine@aards.local")
                .role(Role.FACULTY).departmentId(dept.getId()).active(true).build());
        subjectFacultyRepository.save(SubjectFaculty.builder()
                .subject(se201).faculty(faculty).academicSession(session).build());

        // avg: 8.9, 7.0, 6.5, 6.0, 5.0, 8.0, null, 7.8, 7.9, null
        Student s1 = addStudent("SEC100", "Combine One", dept.getId(), session, 9.0, 8.8);
        Student s2 = addStudent("SEC101", "Combine Two", dept.getId(), session, 7.0, 7.0);
        Student s3 = addStudent("SEC102", "Combine Three", dept.getId(), session, 6.5, 6.5);
        Student s4 = addStudent("SEC103", "Combine Four", dept.getId(), session, 6.0, 6.0);
        Student s5 = addStudent("SEC104", "Combine Five", dept.getId(), session, 5.0, 5.0);
        Student s6 = addStudent("SEC105", "Combine Six", dept.getId(), session, 8.0, null);
        Student s7 = addStudent("SEC106", "Combine Seven", dept.getId(), session, null, null);
        Student s8 = addStudent("SEC107", "Combine Eight", dept.getId(), session, 7.8, 7.8);
        Student s9 = addStudent("SEC108", "Combine Nine", dept.getId(), session, 7.9, 7.9);
        Student s10 = addStudent("SEC109", "Combine Ten", dept.getId(), session, null, null);

        // Grade bands on SE201: Dist, First, HigherSecond, Second, Pass.
        addResult(s1, se201, session, 70, "A", ResultStatus.PASS);
        addResult(s2, se201, session, 62, "B+", ResultStatus.PASS);
        addResult(s3, se201, session, 57, "B", ResultStatus.PASS);
        addResult(s4, se201, session, 52, "C", ResultStatus.PASS);
        addResult(s5, se201, session, 45, "P", ResultStatus.PASS);
        // Backlogs on SE202.
        addResult(s1, se202, session, 80, "A+", ResultStatus.PASS);
        addResult(s7, se202, session, 30, "F", ResultStatus.FAIL);
        addResult(s7, se202, session, 0, null, ResultStatus.ABSENT);
        addResult(s8, se202, session, 28, "F", ResultStatus.FAIL);
        addResult(s8, se202, session, 25, "F", ResultStatus.FAIL);
        // Pass rows so s8/s9 are ATKT (not all-fail): kept on SE203 so the
        // SE201/SE202 table numbers above stay exact.
        addResult(s8, se203, session, 60, "B+", ResultStatus.PASS);
        addResult(s8, se203, session, 61, "B+", ResultStatus.PASS);
        addResult(s8, se203, session, 62, "B+", ResultStatus.PASS);
        // Extra passes so s3-s6 earn 6+ credits (year max is 12, fail below 6).
        addResult(s3, se203, session, 60, "B+", ResultStatus.PASS);
        addResult(s4, se203, session, 61, "B+", ResultStatus.PASS);
        addResult(s5, se203, session, 62, "B+", ResultStatus.PASS);
        addResult(s6, se203, session, 63, "B+", ResultStatus.PASS);
        for (int i = 0; i < 5; i++) {
            addResult(s9, se202, session, 20, "F", ResultStatus.FAIL);
        }
        addResult(s9, se203, session, 70, "A", ResultStatus.PASS);
        addResult(s9, se203, session, 71, "A", ResultStatus.PASS);
        for (int i = 0; i < 3; i++) {
            addResult(s10, se202, session, 22, "F", ResultStatus.FAIL);
        }
        // TUT-only subject: passing grades below the 40% line. These rows are
        // saved PASS, so backlog and all-clear counts must not move.
        Subject se204 = subjectRepository.save(Subject.builder()
                .code("SE204").name("Combine TUT Subject")
                .departmentId(dept.getId()).year(2).semester(3)
                .credits(3).maxMarks(50).passingMarks(20).build());
        addResult(s1, se204, session, 23, "A+", ResultStatus.PASS);
        addResult(s2, se204, session, 38, "A", ResultStatus.PASS);
        addResult(s6, se204, session, 65, "A", ResultStatus.PASS);

        AnalyticsFilterRequest filter = AnalyticsFilterRequest.builder()
                .academicSessionId(session.getId())
                .departmentId(dept.getId())
                .year(2)
                .semester(3)
                .build();
        SECombineReportResponse response = seCombineReportService.generateSECombineReport(filter);

        // Distribution covers all-clear students only: bands add up to All Clear.
        assertEquals(2, response.getDistribution().getDistinction().getCount());
        assertEquals(20.0, response.getDistribution().getDistinction().getPercentage());
        assertEquals(1, response.getDistribution().getFirstClass().getCount());
        assertEquals(1, response.getDistribution().getHigherSecond().getCount());
        assertEquals(1, response.getDistribution().getSecondClass().getCount());
        assertEquals(1, response.getDistribution().getPassClass().getCount());

        // Backlog buckets.
        assertEquals(1, response.getBacklog().getFailedInOne());
        assertEquals(1, response.getBacklog().getFailedInTwo());
        assertEquals(1, response.getBacklog().getFailedInThree());
        assertEquals(0, response.getBacklog().getFailedInFour());
        assertEquals(1, response.getBacklog().getFailedInFiveOrMore());

        // Overall: 10 appeared, 6 clear, 3 quality, 2 ATKT, 2 fail, 0 absent.
        // No YearResult rows exist here so every student uses the explicit
        // credit fallback. s7 has one ABSENT row but also a real FAIL row, so
        // it appeared and is NOT a wholly-absent student: absent counts unique
        // wholly-absent students, never ABSENT subject rows.
        assertEquals(10, response.getOverall().getTotalAppeared());
        assertEquals(6, response.getOverall().getAllClear());
        assertEquals(60.0, response.getOverall().getAllClearPct());
        assertEquals(3, response.getOverall().getQuality());
        assertEquals(30.0, response.getOverall().getQualityPct());
        assertEquals(2, response.getOverall().getWithAtkt());
        assertEquals(20.0, response.getOverall().getWithAtktPct());
        assertEquals(2, response.getOverall().getFail());
        assertEquals(20.0, response.getOverall().getFailPct());
        assertEquals(0, response.getOverall().getAbsent());

        // Semester blocks: II first, then I.
        List<SECombineReportResponse.SemesterBlock> blocks = response.getSemesters();
        assertEquals(2, blocks.size());
        assertEquals("Semester II", blocks.get(0).getSemesterDisplayName());
        assertEquals(0, blocks.get(0).getSubjects().size());
        assertEquals("Semester I", blocks.get(1).getSemesterDisplayName());
        List<SECombineReportResponse.SubjectRow> rows = blocks.get(1).getSubjects();
        assertEquals(4, rows.size());
        assertEquals("SE201", rows.get(0).getSubjectCode());
        assertEquals("Combine Faculty", rows.get(0).getFacultyName());
        assertEquals(5, rows.get(0).getOnRoll());
        assertEquals(5, rows.get(0).getAppeared());
        assertEquals(5, rows.get(0).getPassed());
        assertEquals(100.0, rows.get(0).getPassingPercentage());
        assertEquals(1, rows.get(0).getDistinction());
        assertEquals(1, rows.get(0).getFirstClass());
        assertEquals(1, rows.get(0).getHigherSecond());
        assertEquals(1, rows.get(0).getSecondClass());
        assertEquals(1, rows.get(0).getPassClass());
        assertEquals(70.0, rows.get(0).getHighestMarks());
        assertEquals("SE202", rows.get(1).getSubjectCode());
        assertEquals("—", rows.get(1).getFacultyName());
        assertEquals(5, rows.get(1).getOnRoll());
        assertEquals(5, rows.get(1).getAppeared());
        assertEquals(1, rows.get(1).getPassed());
        assertEquals(20.0, rows.get(1).getPassingPercentage());
        assertEquals("SE203", rows.get(2).getSubjectCode());
        assertEquals(6, rows.get(2).getOnRoll());
        assertEquals(6, rows.get(2).getAppeared());
        assertEquals(6, rows.get(2).getPassed());
        assertEquals(100.0, rows.get(2).getPassingPercentage());
        assertEquals(2, rows.get(2).getDistinction());
        assertEquals(7, rows.get(2).getFirstClass());
        assertEquals("SE204", rows.get(3).getSubjectCode());
        assertEquals(3, rows.get(3).getOnRoll());
        assertEquals(3, rows.get(3).getAppeared());
        assertEquals(3, rows.get(3).getPassed());
        assertEquals(100.0, rows.get(3).getPassingPercentage());
        // Bands run on percentage of maxMarks (50): 23->pass, 38->dist, 65->dist.
        assertEquals(2, rows.get(3).getDistinction());
        assertEquals(1, rows.get(3).getPassClass());
        assertEquals(65.0, rows.get(3).getHighestMarks());

        // Toppers: top 5 by avg SGPA.
        List<SECombineReportResponse.TopperRow> toppers = response.getToppers();
        assertEquals(5, toppers.size());
        assertEquals("Combine One", toppers.get(0).getName());
        assertEquals(1, toppers.get(0).getRank());
        assertEquals("Combine Six", toppers.get(1).getName());
        assertEquals("Combine Nine", toppers.get(2).getName());
        assertEquals("Combine Eight", toppers.get(3).getName());
        assertEquals("Combine Two", toppers.get(4).getName());
        assertEquals(5, toppers.get(4).getRank());
    }

    // Official year result is authoritative: it overrides the credit/backlog
    // fallback (O2 fails despite clean rows, O3 is ATKT despite zero FAIL
    // rows), unknown/missing officials use the explicit fallback, and absent
    // counts wholly-absent students only (O6: three ABSENT rows, one student).
    @Test
    void officialResultDrivesClassification() {
        AcademicSession session = sessionRepository.save(AcademicSession.builder()
                .name("2036-37").active(true).build());
        Department dept = departmentRepository.save(Department.builder()
                .name("Official Dept").code("OMB").build());
        Subject sx1 = subjectRepository.save(Subject.builder()
                .code("SX1").name("Official Subject One")
                .departmentId(dept.getId()).year(2).semester(3)
                .credits(3).maxMarks(100).passingMarks(40).build());
        Subject sx2 = subjectRepository.save(Subject.builder()
                .code("SX2").name("Official Subject Two")
                .departmentId(dept.getId()).year(2).semester(3)
                .credits(3).maxMarks(100).passingMarks(40).build());

        Student o1 = addStudent("SEO200", "Official Clear", dept.getId(), session, 8.0, 8.0);
        addResult(o1, sx1, session, 70, "A", ResultStatus.PASS);
        addResult(o1, sx2, session, 70, "A", ResultStatus.PASS);
        addOfficial(o1, session, null, YearResultStatus.ALL_CLEAR);

        // Ledger Fail with 44/44-style clean rows: official wins, and the
        // student stays out of the distinction bands.
        Student o2 = addStudent("SEO201", "Official Fail Clean", dept.getId(), session, 8.5, 8.5);
        addResult(o2, sx1, session, 70, "A", ResultStatus.PASS);
        addResult(o2, sx2, session, 70, "A", ResultStatus.PASS);
        addOfficial(o2, session, "Fail", YearResultStatus.FAIL);

        Student o3 = addStudent("SEO202", "Official Atkt Clean", dept.getId(), session, 7.0, 7.0);
        addResult(o3, sx1, session, 65, "A", ResultStatus.PASS);
        addResult(o3, sx2, session, 65, "A", ResultStatus.PASS);
        addOfficial(o3, session, "Fail A.T.K.T.", YearResultStatus.ATKT);

        Student o4 = addStudent("SEO203", "Missing Official", dept.getId(), session, 7.0, 7.0);
        addResult(o4, sx1, session, 65, "A", ResultStatus.PASS);
        addResult(o4, sx2, session, 65, "A", ResultStatus.PASS);

        Student o5 = addStudent("SEO204", "Unknown Official", dept.getId(), session, 6.0, 6.0);
        addResult(o5, sx1, session, 65, "A", ResultStatus.PASS);
        addResult(o5, sx2, session, 20, "F", ResultStatus.FAIL);
        addOfficial(o5, session, "Withheld", YearResultStatus.UNKNOWN);

        Student o6 = addStudent("SEO205", "Wholly Absent", dept.getId(), session, null, null);
        addResult(o6, sx1, session, 0, null, ResultStatus.ABSENT);
        addResult(o6, sx1, session, 0, null, ResultStatus.ABSENT);
        addResult(o6, sx1, session, 0, null, ResultStatus.ABSENT);

        // One AAA row next to real passes: appeared, never absent.
        Student o7 = addStudent("SEO206", "Single Aaa Appeared", dept.getId(), session, 5.0, 5.0);
        addResult(o7, sx1, session, 45, "P", ResultStatus.PASS);
        addResult(o7, sx2, session, 45, "P", ResultStatus.PASS);
        addResult(o7, sx2, session, 0, null, ResultStatus.ABSENT);

        AnalyticsFilterRequest filter = AnalyticsFilterRequest.builder()
                .academicSessionId(session.getId())
                .departmentId(dept.getId())
                .year(2)
                .semester(3)
                .build();
        SECombineReportResponse response = seCombineReportService.generateSECombineReport(filter);

        assertEquals(1, response.getOverall().getAbsent());
        assertEquals(6, response.getOverall().getTotalAppeared());
        assertEquals(3, response.getOverall().getAllClear());
        assertEquals(50.0, response.getOverall().getAllClearPct());
        assertEquals(2, response.getOverall().getQuality());
        assertEquals(2, response.getOverall().getWithAtkt());
        assertEquals(1, response.getOverall().getFail());
        // O2 (official Fail, avg 8.5) stays out of the bands: only O1 is Distinction.
        assertEquals(1, response.getDistribution().getDistinction().getCount());
        assertEquals(1, response.getDistribution().getFirstClass().getCount());
        assertEquals(1, response.getDistribution().getPassClass().getCount());
        assertEquals(1, response.getBacklog().getFailedInOne());

        List<SECombineReconciliationRow> rows =
                seCombineReportService.reconcileSECombine(filter);
        assertEquals(7, rows.size());
        Map<String, SECombineReconciliationRow> byPrn = rows.stream()
                .collect(Collectors.toMap(SECombineReconciliationRow::getPrn, r -> r));

        assertRow(byPrn.get("SEO200"), null, YearResultStatus.ALL_CLEAR,
                true, YearResultStatus.ALL_CLEAR, false);
        assertRow(byPrn.get("SEO201"), "Fail", YearResultStatus.FAIL,
                true, YearResultStatus.FAIL, false);
        assertRow(byPrn.get("SEO202"), "Fail A.T.K.T.", YearResultStatus.ATKT,
                true, YearResultStatus.ATKT, false);
        assertRow(byPrn.get("SEO203"), null, null,
                true, YearResultStatus.ALL_CLEAR, true);
        assertRow(byPrn.get("SEO204"), "Withheld", YearResultStatus.UNKNOWN,
                true, YearResultStatus.ATKT, true);
        assertRow(byPrn.get("SEO205"), null, null,
                false, YearResultStatus.ABSENT, true);
        assertRow(byPrn.get("SEO206"), null, null,
                true, YearResultStatus.ALL_CLEAR, true);

        assertEquals(4, rows.stream().filter(SECombineReconciliationRow::isFallbackUsed).count());
        List<String> unknownRaws = rows.stream()
                .filter(r -> r.getNormalizedOfficialResult() == YearResultStatus.UNKNOWN)
                .map(SECombineReconciliationRow::getRawOfficialResult).toList();
        assertEquals(List.of("Withheld"), unknownRaws);
    }

    private static void assertRow(SECombineReconciliationRow row, String raw,
                                 YearResultStatus normalized, boolean appeared,
                                 YearResultStatus finalStatus, boolean fallbackUsed) {
        assertTrue(row != null);
        assertEquals(raw, row.getRawOfficialResult());
        assertEquals(normalized, row.getNormalizedOfficialResult());
        assertEquals(appeared, row.isAppeared());
        assertEquals(finalStatus, row.getFinalClassification());
        assertEquals(fallbackUsed, row.isFallbackUsed());
    }
}
