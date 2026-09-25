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

// Uses ledger-style text (2019 Pattern): 1 student, 2 subjects, SGPA + result lines.
@SpringBootTest
class ParserServiceTest {

    @Autowired
    private ParserService parserService;

    private static final String LEDGER_TEXT =
            "PRN: 72332766B Seat No.: F190890003 NAME: RAHUL SHARMA Mother- SUNITA\n"
            + "SEMESTER: 1\n"
            + "101011- 1 P 014 P 028 --- --- --- --- 042 3 3 P 4 12\n"
            + "102003- 1 P 016 P 041 --- --- --- 057 3 3 B+ 7 21\n"
            + "First Semester SGPA : 7.14 Credits Earned/Total : 22/22 Total Credit Points: 157\n"
            + "First Year Total Credits Earned : 44/44";

    @Test
    void parseLedgerText() {
        List<ParsedRecord> records = parserService.parseText(LEDGER_TEXT);

        assertEquals(1, records.size());
        ParsedRecord record = records.get(0);
        assertEquals("72332766B", record.getPrn());
        assertEquals("RAHUL SHARMA", record.getName());
        assertEquals(2, record.getMarks().size());
        assertEquals("101011-1", record.getMarks().get(0).getSubjectCode());
        assertEquals("102003-1", record.getMarks().get(1).getSubjectCode());
        assertEquals(1, record.getSemesters().size());
        assertEquals(7.14, record.getSemesters().get(0).getSgpa());
        assertEquals("PASS", record.getOverallResult());
    }

    @Test
    void parseLedgerPdf() throws Exception {
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
                for (String line : LEDGER_TEXT.split("\\n")) {
                    content.showText(line);
                    content.newLine();
                }
                content.endText();
            }
            doc.save(out);
            pdfBytes = out.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile(
                "file", "ledger.pdf", "application/pdf", pdfBytes);
        List<ParsedRecord> records = parserService.parse(file);

        assertEquals(1, records.size());
        assertEquals("72332766B", records.get(0).getPrn());
    }
}
