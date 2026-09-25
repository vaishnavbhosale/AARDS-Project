package com.aards.parser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Builds a tiny PDF in memory and checks the parser finds 2 students.
@SpringBootTest
class ParserServiceTest {

    @Autowired
    private ParserService parserService;

    @Test
    void parseTwoStudents() throws Exception {
        byte[] pdfBytes = buildFakePdf();

        MockMultipartFile file = new MockMultipartFile(
                "file", "result.pdf", "application/pdf", pdfBytes);

        List<ParsedRecord> records = parserService.parse(file);

        assertEquals(2, records.size());
        assertEquals("11111111", records.get(0).getPrn());
        assertEquals("22222222", records.get(1).getPrn());
        assertEquals(2, records.get(0).getMarks().size());
        assertEquals(2, records.get(1).getMarks().size());
        assertEquals(75.0, records.get(0).getMarks().get(0).getMarks());
        assertEquals("CS201", records.get(0).getMarks().get(0).getSubjectCode());
    }

    private byte[] buildFakePdf() throws Exception {
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
                        "PRN: 11111111 Name: Aarav Sharma Year: 2 Sem: 3",
                        "CS201 75/100 A",
                        "CS202 62/100 B",
                        "PRN: 22222222 Name: Diya Patil Year: 2 Sem: 3",
                        "CS201 82/100 A",
                        "CS202 55/100 C"
                };
                for (String line : lines) {
                    content.showText(line);
                    content.newLine();
                }
                content.endText();
            }
            doc.save(out);
            return out.toByteArray();
        }
    }
}
