package com.aards.analytics;

import com.aards.analytics.dto.AnalyticsFilterRequest;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
                .credits(2).maxMarks(100).passingMarks(40).build());
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

        // Overall: 10 appeared, 6 clear, 3 quality, 2 ATKT, 2 fail, 1 absent row.
        assertEquals(10, response.getOverall().getTotalAppeared());
        assertEquals(6, response.getOverall().getAllClear());
        assertEquals(60.0, response.getOverall().getAllClearPct());
        assertEquals(3, response.getOverall().getQuality());
        assertEquals(30.0, response.getOverall().getQualityPct());
        assertEquals(2, response.getOverall().getWithAtkt());
        assertEquals(20.0, response.getOverall().getWithAtktPct());
        assertEquals(2, response.getOverall().getFail());
        assertEquals(20.0, response.getOverall().getFailPct());
        assertEquals(1, response.getOverall().getAbsent());

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
        assertEquals(2, rows.get(2).getOnRoll());
        assertEquals(2, rows.get(2).getAppeared());
        assertEquals(2, rows.get(2).getPassed());
        assertEquals(100.0, rows.get(2).getPassingPercentage());
        assertEquals(2, rows.get(2).getDistinction());
        assertEquals(3, rows.get(2).getFirstClass());
        assertEquals("SE204", rows.get(3).getSubjectCode());
        assertEquals(3, rows.get(3).getOnRoll());
        assertEquals(3, rows.get(3).getAppeared());
        assertEquals(3, rows.get(3).getPassed());
        assertEquals(100.0, rows.get(3).getPassingPercentage());

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
}
