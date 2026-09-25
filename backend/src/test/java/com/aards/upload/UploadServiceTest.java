package com.aards.upload;

import com.aards.student.StudentRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Uploads 1 student PDF and checks batch + student are saved.
@SpringBootTest
@Transactional
class UploadServiceTest {

    @Autowired
    private UploadService uploadService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Test
    void uploadOneStudent() throws Exception {
        User admin = userRepository.findByUsername("admin").orElseThrow();

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
                        "PRN: 33334444 Name: Test Student Year: 2 Sem: 3",
                        "CS201 70/100 B"
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

        UploadBatchResponse response = uploadService.processUpload(file, admin);

        assertNotNull(response.getId());
        assertEquals(1, response.getTotalRecords());
        assertTrue(response.getStatus().equals("VALIDATED") || response.getStatus().equals("PARSED"));
        assertTrue(studentRepository.findByPrn("33334444").isPresent());
    }
}
