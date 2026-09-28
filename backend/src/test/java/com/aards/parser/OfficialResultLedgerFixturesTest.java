package com.aards.parser;

import com.aards.yearresult.YearResultStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Synthetic single-student SE NEP 2020 ledger blocks under
// src/test/resources/ledger. They pin the verified official-result shapes
// (PDFBox extraction of DownloadLedger.pdf):
// all-clear = Total-only line, ATKT = "Fail A.T.K.T.", Fail = "Fail".
@SpringBootTest
class OfficialResultLedgerFixturesTest {

    @Autowired
    private ParserService parserService;

    private ParsedRecord parseFixture(String name) throws Exception {
        String text;
        try (var in = getClass().getResourceAsStream("/ledger/" + name)) {
            assertTrue(in != null, "missing fixture " + name);
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        List<ParsedRecord> records = parserService.parseText(text);
        assertEquals(1, records.size(), "fixture " + name);
        return records.get(0);
    }

    @Test
    void allClearHasNoResultValue() throws Exception {
        ParsedRecord record = parseFixture("se-all-clear.txt");
        assertEquals("724290001", record.getPrn());
        assertNull(record.getOfficialResultRaw());
        assertEquals(2, record.getOfficialResultYear());
        assertEquals(44, record.getOfficialCreditsEarned());
        assertEquals(44, record.getOfficialTotalCredits());
        assertEquals(YearResultStatus.ALL_CLEAR, YearResultStatus.fromLedger(
                record.getOfficialResultRaw(),
                record.getOfficialCreditsEarned(), record.getOfficialTotalCredits()));
        assertEquals("PASS", record.getOverallResult());
    }

    @Test
    void atktKeepsVerbatimRawValue() throws Exception {
        ParsedRecord record = parseFixture("se-atkt.txt");
        assertEquals("Fail A.T.K.T.", record.getOfficialResultRaw());
        assertEquals(2, record.getOfficialResultYear());
        assertEquals(38, record.getOfficialCreditsEarned());
        assertEquals(44, record.getOfficialTotalCredits());
        assertEquals(YearResultStatus.ATKT, YearResultStatus.fromLedger(
                record.getOfficialResultRaw(),
                record.getOfficialCreditsEarned(), record.getOfficialTotalCredits()));
        assertEquals("FAIL", record.getOverallResult());
    }

    @Test
    void failKeepsVerbatimRawValue() throws Exception {
        ParsedRecord record = parseFixture("se-fail.txt");
        assertEquals("Fail", record.getOfficialResultRaw());
        assertEquals(YearResultStatus.FAIL, YearResultStatus.fromLedger(
                record.getOfficialResultRaw(),
                record.getOfficialCreditsEarned(), record.getOfficialTotalCredits()));
    }

    @Test
    void whollyAbsentStudentHasOnlyAbsentRows() throws Exception {
        ParsedRecord record = parseFixture("se-absent.txt");
        assertNull(record.getOfficialResultRaw());
        assertEquals(0, record.getOfficialCreditsEarned());
        assertEquals(44, record.getOfficialTotalCredits());
        // Partial credits with no Result value: never guessed, stays UNKNOWN.
        assertEquals(YearResultStatus.UNKNOWN, YearResultStatus.fromLedger(
                record.getOfficialResultRaw(),
                record.getOfficialCreditsEarned(), record.getOfficialTotalCredits()));
        assertTrue(record.getMarks().size() >= 2);
        for (SubjectMark mark : record.getMarks()) {
            assertEquals("ABSENT", mark.getStatus());
        }
    }

    @Test
    void singleAaaRowDoesNotHideOfficialAtkt() throws Exception {
        ParsedRecord record = parseFixture("se-aaa-single-atkt.txt");
        assertEquals("Fail A.T.K.T.", record.getOfficialResultRaw());
        long absentRows = record.getMarks().stream()
                .filter(m -> "ABSENT".equals(m.getStatus())).count();
        assertEquals(1, absentRows);
        assertTrue(record.getMarks().size() > 1);
        assertEquals(YearResultStatus.ATKT, YearResultStatus.fromLedger(
                record.getOfficialResultRaw(),
                record.getOfficialCreditsEarned(), record.getOfficialTotalCredits()));
    }

    @Test
    void missingSecondYearLineStaysUnknown() throws Exception {
        ParsedRecord record = parseFixture("se-missing-result.txt");
        assertNull(record.getOfficialResultRaw());
        assertNull(record.getOfficialResultYear());
        assertNull(record.getOfficialCreditsEarned());
        assertNull(record.getOfficialTotalCredits());
        assertEquals(YearResultStatus.UNKNOWN, YearResultStatus.fromLedger(
                record.getOfficialResultRaw(),
                record.getOfficialCreditsEarned(), record.getOfficialTotalCredits()));
        assertEquals("UNKNOWN", record.getOverallResult());
    }

    @Test
    void unknownResultStringIsPreservedNotGuessed() throws Exception {
        ParsedRecord record = parseFixture("se-unknown-result.txt");
        assertEquals("Withheld", record.getOfficialResultRaw());
        assertEquals(YearResultStatus.UNKNOWN, YearResultStatus.fromLedger(
                record.getOfficialResultRaw(),
                record.getOfficialCreditsEarned(), record.getOfficialTotalCredits()));
        assertEquals("UNKNOWN", record.getOverallResult());
    }

    @Test
    void absentRowsAcrossBothSemestersStayAbsent() throws Exception {
        ParsedRecord record = parseFixture("se-absent-multi-rows.txt");
        assertEquals(6, record.getMarks().size());
        for (SubjectMark mark : record.getMarks()) {
            assertEquals("ABSENT", mark.getStatus());
        }
        assertEquals(YearResultStatus.UNKNOWN, YearResultStatus.fromLedger(
                record.getOfficialResultRaw(),
                record.getOfficialCreditsEarned(), record.getOfficialTotalCredits()));
    }
}
