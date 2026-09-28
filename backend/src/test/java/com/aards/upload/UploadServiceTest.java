package com.aards.upload;

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
import com.aards.user.Role;
import com.aards.user.User;
import com.aards.user.repository.UserRepository;
import com.aards.yearresult.YearResult;
import com.aards.yearresult.YearResultRepository;
import com.aards.yearresult.YearResultStatus;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// submitUpload returns at once; the pipeline finishes on a background thread.
// No @Transactional here: the background thread cannot see uncommitted rows,
// so this test commits setup and cleans up after itself.
@SpringBootTest
class UploadServiceTest {

    private static final Set<String> TERMINAL =
            Set.of("VALIDATED", "PARSED", "FAILED");

    @Autowired
    private UploadService uploadService;

    @Autowired
    private UploadBatchRepository batchRepository;

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

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private YearResultRepository yearResultRepository;

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
                        "Semester: 1",
                        "Code Paper Title",
                        "101011- 1 101011 Engineering Mechanics",
                        "PRN: 33334444A Seat No.: F190890001 NAME: Test Student Mother- Test Mother",
                        "SEMESTER: 1",
                        "101011- 1 P 014 P 028 --- --- --- --- 042 3 3 P 4 12",
                        "EEM-231-ETC  --- --- --- --- --- P 023 --- --- --- --- --- --- 023  2  2  A+  9  18",
                        "PCC-201-ETC  P 024 * 033 --- --- --- --- --- --- --- --- --- --- 057  4  4  B+  7  28",
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

        // Returns immediately, before the background job finishes.
        UploadBatchResponse submitted = uploadService.submitUpload(file, faculty);

        assertNotNull(submitted.getId());
        assertTrue(submitted.getStatus().equals("UPLOADED")
                || submitted.getStatus().equals("PARSING"));

        // Wait (up to ~10s) for the background thread to finish.
        UploadBatchResponse latest = null;
        for (int i = 0; i < 20; i++) {
            Thread.sleep(500);
            latest = uploadService.getById(submitted.getId());
            if (TERMINAL.contains(latest.getStatus())) {
                break;
            }
        }

        try {
            assertNotNull(latest);
            assertEquals("VALIDATED", latest.getStatus());
            assertEquals(1, latest.getTotalRecords());
            assertEquals("Computer Engineering", latest.getDepartmentName());
            assertEquals(active.getName(), latest.getAcademicSessionName());

            // Student is tagged with the faculty department + year/sem from the record.
            Student student = studentRepository.findByPrn("33334444A").orElseThrow();
            assertEquals(comp.getId(), student.getDepartmentId());
            assertEquals(1, student.getCurrentYear());
            assertEquals(1, student.getCurrentSemester());
            assertEquals(LocalDate.now().getYear(), student.getAdmissionYear());

            // Every result row uses the active session + record year/sem.
            List<Result> results = resultRepository
                    .findByStudentIdAndAcademicSessionId(student.getId(), active.getId());
            assertEquals(3, results.size());
            assertEquals(1, results.get(0).getYear());
            assertEquals(1, results.get(0).getSemester());

            // TUT-only row: passing grade below the 40% line still saves PASS.
            Subject tutSubject = subjectRepository
                    .findByCodeAndDepartmentId("EEM-231-ETC", comp.getId())
                    .orElseThrow();
            Result tutResult = resultRepository
                    .findBySubjectIdAndAcademicSessionId(tutSubject.getId(), active.getId())
                    .stream().findFirst().orElseThrow();
            assertEquals("A+", tutResult.getGrade());
            assertEquals(ResultStatus.PASS, tutResult.getStatus());

            // Ledger credits flow onto the Subject (4, not the default 3).
            Subject fourCredit = subjectRepository
                    .findByCodeAndDepartmentId("PCC-201-ETC", comp.getId())
                    .orElseThrow();
            assertEquals(4, fourCredit.getCredits());

            // Saved subject uses the list-page title, not the code.
            Subject subject = subjectRepository
                    .findByCodeAndDepartmentIdAndYearAndSemester(
                            "101011-1", comp.getId(), 1, 1)
                    .orElseThrow();
            assertEquals("Engineering Mechanics", subject.getName());

            // One semester row with the ledger SGPA and zero backlogs (grade P).
            SemesterResult semesterResult = semesterResultRepository
                    .findByStudentIdAndAcademicSessionIdAndYearAndSemester(
                            student.getId(), active.getId(), 1, 1)
                    .orElseThrow();
            assertEquals(1, semesterResult.getSemester());
            assertEquals(7.50, semesterResult.getSgpa());
            assertEquals(0, semesterResult.getBacklogCount());
            assertEquals(SemesterStatus.PASS, semesterResult.getStatus());

            // Official year result persisted: Total-only 44/44 trailer, no
            // Result value -> normalized ALL_CLEAR with null raw.
            YearResult yearResult = yearResultRepository
                    .findByStudentIdAndAcademicSessionIdAndYear(
                            student.getId(), active.getId(), 1)
                    .orElseThrow();
            assertNull(yearResult.getOfficialResultRaw());
            assertEquals(YearResultStatus.ALL_CLEAR, yearResult.getStatus());
        } finally {
            // Background thread commits on its own: delete what it saved.
            studentRepository.findByPrn("33334444A").ifPresent(student -> {
                resultRepository.deleteAll(resultRepository
                        .findByStudentIdAndAcademicSessionId(student.getId(), active.getId()));
                semesterResultRepository
                        .findByStudentIdAndAcademicSessionIdAndYearAndSemester(
                                student.getId(), active.getId(), 1, 1)
                        .ifPresent(semesterResultRepository::delete);
                yearResultRepository.findByStudentIdAndAcademicSessionIdAndYear(
                                student.getId(), active.getId(), 1)
                        .ifPresent(yearResultRepository::delete);
                studentRepository.delete(student);
            });
            batchRepository.deleteById(submitted.getId());
        }
    }
}
