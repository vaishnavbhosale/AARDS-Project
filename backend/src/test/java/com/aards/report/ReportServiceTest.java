package com.aards.report;

import com.aards.analytics.dto.AnalyticsFilterRequest;
import com.aards.department.Department;
import com.aards.department.DepartmentRepository;
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
import com.aards.subject.SubjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Builds a small fixture (3 students, 1 subject) and checks each report
// comes back as a real non-empty PDF. Content is not asserted.
@SpringBootTest
@Transactional
class ReportServiceTest {

    @Autowired
    private ReportService reportService;

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

    private Fixture setupFixture() {
        AcademicSession session = sessionRepository.save(AcademicSession.builder()
                .name("2040-41").active(true).build());
        Department dept = departmentRepository.save(Department.builder()
                .name("Report Dept").code("RPTX").build());

        double[] sgpas = {9.0, 8.0, 6.0};
        String[] names = {"Report Topper", "Report Second", "Report Third"};
        Student first = null;
        for (int i = 0; i < 3; i++) {
            Student student = studentRepository.save(Student.builder()
                    .prn("RPT900" + i)
                    .rollNumber("RR" + i)
                    .fullName(names[i])
                    .departmentId(dept.getId())
                    .currentYear(2)
                    .currentSemester(3)
                    .active(true)
                    .build());
            if (i == 0) {
                first = student;
            }
            semesterResultRepository.save(SemesterResult.builder()
                    .studentId(student.getId())
                    .academicSessionId(session.getId())
                    .year(2)
                    .semester(3)
                    .sgpa(sgpas[i])
                    .backlogCount(i == 2 ? 1 : 0)
                    .status(i == 2 ? SemesterStatus.FAIL : SemesterStatus.PASS)
                    .build());
        }

        Subject subject = subjectRepository.save(Subject.builder()
                .code("RP101").name("Report Subject")
                .departmentId(dept.getId()).year(2).semester(3)
                .credits(4).maxMarks(100).passingMarks(40).build());
        resultRepository.save(Result.builder()
                .student(first).subject(subject).academicSession(session)
                .year(2).semester(3)
                .marksObtained(80.0).grade("A")
                .status(ResultStatus.PASS).backlog(false).build());

        AnalyticsFilterRequest filter = AnalyticsFilterRequest.builder()
                .academicSessionId(session.getId())
                .departmentId(dept.getId())
                .year(2)
                .semester(3)
                .build();
        return new Fixture(filter, subject.getId(), session.getId());
    }

    @Test
    void departmentReportIsPdf() {
        Fixture f = setupFixture();
        byte[] pdf = reportService.generateDepartmentReport(f.filter());

        assertNotNull(pdf);
        assertTrue(pdf.length > 500);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    void subjectReportIsPdf() {
        Fixture f = setupFixture();
        byte[] pdf = reportService.generateSubjectReport(f.subjectId(), f.filter());

        assertNotNull(pdf);
        assertTrue(pdf.length > 500);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    void instituteReportIsPdf() {
        Fixture f = setupFixture();
        byte[] pdf = reportService.generateInstituteReport(
                f.sessionId(), f.filter().getYear(), f.filter().getSemester());

        assertNotNull(pdf);
        assertTrue(pdf.length > 500);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    void emptyDepartmentReportStillProducesPdf() {
        Fixture f = setupFixture();
        AnalyticsFilterRequest empty = AnalyticsFilterRequest.builder()
                .academicSessionId(f.filter().getAcademicSessionId())
                .departmentId(f.filter().getDepartmentId())
                .year(4)
                .semester(8)
                .build();
        byte[] pdf = reportService.generateDepartmentReport(empty);

        assertNotNull(pdf);
        assertTrue(pdf.length > 500);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    private record Fixture(AnalyticsFilterRequest filter, Long subjectId, Long sessionId) {
    }
}
