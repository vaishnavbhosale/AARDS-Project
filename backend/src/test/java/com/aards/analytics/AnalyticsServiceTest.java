package com.aards.analytics;

import com.aards.analytics.dto.AnalyticsFilterRequest;
import com.aards.analytics.dto.DashboardResponse;
import com.aards.analytics.dto.FilterOptionsDto;
import com.aards.department.Department;
import com.aards.department.DepartmentRepository;
import com.aards.semesterresult.SemesterResult;
import com.aards.semesterresult.SemesterResultRepository;
import com.aards.semesterresult.SemesterStatus;
import com.aards.session.AcademicSession;
import com.aards.session.AcademicSessionRepository;
import com.aards.student.Student;
import com.aards.student.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

// Builds 5 students (3 pass, 2 fail) and checks dashboard maths.
@SpringBootTest
@Transactional
class AnalyticsServiceTest {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private AcademicSessionRepository sessionRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SemesterResultRepository semesterResultRepository;

    @Test
    void dashboardCardsAreCorrect() {
        AcademicSession session = sessionRepository.save(AcademicSession.builder()
                .name("2030-31").active(true).build());
        Department dept = departmentRepository.save(Department.builder()
                .name("Test Dept").code("TST").build());

        // 5 students: SGPAs 9.2, 8.0, 7.1 (pass) and 6.8, 6.5 (fail).
        double[] sgpas = {9.2, 8.0, 7.1, 6.8, 6.5};
        int[] backlogs = {0, 0, 1, 2, 3};
        String[] names = {"Topper Student", "Second Student", "Third Student",
                "Fourth Student", "Fifth Student"};
        for (int i = 0; i < 5; i++) {
            Student student = studentRepository.save(Student.builder()
                    .prn("ANAL000" + i)
                    .rollNumber("R" + i)
                    .fullName(names[i])
                    .departmentId(dept.getId())
                    .currentYear(2)
                    .currentSemester(3)
                    .active(true)
                    .build());
            semesterResultRepository.save(SemesterResult.builder()
                    .studentId(student.getId())
                    .academicSessionId(session.getId())
                    .year(2)
                    .semester(3)
                    .sgpa(sgpas[i])
                    .backlogCount(backlogs[i])
                    .status(i < 3 ? SemesterStatus.PASS : SemesterStatus.FAIL)
                    .build());
        }

        AnalyticsFilterRequest filter = AnalyticsFilterRequest.builder()
                .academicSessionId(session.getId())
                .departmentId(dept.getId())
                .year(2)
                .semester(3)
                .build();
        DashboardResponse response = analyticsService.getDashboard(filter);

        assertEquals(5, response.getCards().getTotalStudents());
        assertEquals(3, response.getCards().getPassedStudents());
        assertEquals(2, response.getCards().getFailedStudents());
        assertEquals(60.0, response.getCards().getOverallPassPercentage());
        assertEquals(9.2, response.getCards().getHighestSgpa());
        assertEquals("Topper Student", response.getTopperName());
    }

    @Test
    void filterOptionsAreFilled() {
        FilterOptionsDto options = analyticsService.getFilterOptions();
        assertFalse(options.getSessions().isEmpty());
        assertEquals(4, options.getYears().size());
    }
}
