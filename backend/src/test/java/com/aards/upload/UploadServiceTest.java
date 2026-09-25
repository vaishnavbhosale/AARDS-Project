package com.aards.upload;

import com.aards.department.Department;
import com.aards.department.DepartmentRepository;
import com.aards.result.Result;
import com.aards.result.ResultRepository;
import com.aards.semesterresult.SemesterResult;
import com.aards.semesterresult.SemesterResultRepository;
import com.aards.semesterresult.SemesterStatus;
import com.aards.session.AcademicSession;
import com.aards.session.AcademicSessionRepository;
import com.aards.student.Student;
import com.aards.student.StudentRepository;
import com.aards.user.Role;
import com.aards.user.User;
import com.aards.user.repository.UserRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// Uploads 1 student PDF as a COMP faculty member and checks the saved rows
// are tagged with the faculty department + active session.
@SpringBootTest
@Transactional
class UploadServiceTest {

    @Autowired
    private UploadService uploadService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private AcademicSessionRepository sessionRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private SemesterResultRepository semesterResultRepository;

    @Test
    void uploadOneStudent() throws Exception {
        // Faculty with COMP department (create or reuse, never duplicate).
        Department comp = departmentRepository.findByCode("COMP")
                .orElseGet(() -> departmentRepository.save(Department.builder()
                        .name("Computer Engineering").code("COMP").build()));
        AcademicSession active = sessionRepository.findByActive(true).stream().findFirst()
                .orElseGet(() -> sessionRepository.save(AcademicSession.builder()
                        .name("2024-25").active(true).build()));
        User faculty = userRepository.findByUsername("comp_faculty")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("comp_faculty")
                        .password("test")
                        .fullName("Comp Faculty")
                        .email("comp.faculty@aards.local")
                        .role(Role.FACULTY)
                        .departmentId(comp.getId())
                        .active(true)
                        .build()));

        byte[] pdfBytes;
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(doc, page)) {
                content.setFont(PDType1Font.HELVETICA, 12);
                content.beginText();
                content.setLeading(15f);
                content.newLineAtOffset(50, 750);
                String[] lines = {
                        "PRN: 33334444A Seat No.: F190890001 NAME: Test Student Mother- Test Mother",
                        "SEMESTER: 1",
                        "101011- 1 P 014 P 028 --- --- --- --- 042 3 3 P 4 12",
                        "First Semester SGPA : 7.50 Credits Earned/Total : 22/22 Total Credit Points: 165",
                        "First Year Total Credits Earned : 44/44"
                };
                for (String line : lines) {
                    content.showText(line);
                    content.newLine();
                }
                content.endText();
            }
            doc.save(out);
            pdfBytes = out.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile(
                "file", "one.pdf", "application/pdf", pdfBytes);

        UploadBatchResponse response = uploadService.processUpload(file, faculty);

        assertNotNull(response.getId());
        assertEquals(1, response.getTotalRecords());
        assertEquals("VALIDATED", response.getStatus());
        assertEquals("Computer Engineering", response.getDepartmentName());
        assertEquals(active.getName(), response.getAcademicSessionName());

        // Student is tagged with the faculty department + year/sem from the record.
        Student student = studentRepository.findByPrn("33334444A").orElseThrow();
        assertEquals(comp.getId(), student.getDepartmentId());
        assertEquals(1, student.getCurrentYear());
        assertEquals(1, student.getCurrentSemester());
        assertEquals(LocalDate.now().getYear(), student.getAdmissionYear());

        // Every result row uses the active session + record year/sem.
        List<Result> results = resultRepository
                .findByStudentIdAndAcademicSessionId(student.getId(), active.getId());
        assertEquals(1, results.size());
        assertEquals(1, results.get(0).getYear());
        assertEquals(1, results.get(0).getSemester());

        // One semester row with the ledger SGPA and zero backlogs (grade P).
        SemesterResult semesterResult = semesterResultRepository
                .findByStudentIdAndAcademicSessionIdAndYearAndSemester(
                        student.getId(), active.getId(), 1, 1)
                .orElseThrow();
        assertEquals(1, semesterResult.getSemester());
        assertEquals(7.50, semesterResult.getSgpa());
        assertEquals(0, semesterResult.getBacklogCount());
        assertEquals(SemesterStatus.PASS, semesterResult.getStatus());
    }
}
