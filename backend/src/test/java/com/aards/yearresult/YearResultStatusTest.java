package com.aards.yearresult;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// Pure mapping tests: no Spring, no ledger needed. Normalization is
// trim + single-space + case-insensitive; raw values stay verbatim.
class YearResultStatusTest {

    @Test
    void verifiedLedgerStrings() {
        assertEquals(YearResultStatus.ATKT, YearResultStatus.fromLedger("Fail A.T.K.T.", 38, 44));
        assertEquals(YearResultStatus.FAIL, YearResultStatus.fromLedger("Fail", 19, 44));
        assertEquals(YearResultStatus.ALL_CLEAR, YearResultStatus.fromLedger(null, 44, 44));
    }

    @Test
    void comparisonIgnoresCaseAndWhitespace() {
        assertEquals(YearResultStatus.ATKT, YearResultStatus.fromLedger("  fail   a.t.k.t. ", 38, 44));
        assertEquals(YearResultStatus.ATKT, YearResultStatus.fromLedger("FAIL A.T.K.T.", 38, 44));
        assertEquals(YearResultStatus.ATKT, YearResultStatus.fromLedger("Fail ATKT", 38, 44));
        assertEquals(YearResultStatus.FAIL, YearResultStatus.fromLedger("  FAIL ", 19, 44));
        assertEquals(YearResultStatus.ALL_CLEAR, YearResultStatus.fromLedger("Pass", 44, 44));
    }

    @Test
    void unknownStringsAreNeverGuessed() {
        assertEquals(YearResultStatus.UNKNOWN, YearResultStatus.fromLedger("Withheld", 30, 44));
        assertEquals(YearResultStatus.UNKNOWN, YearResultStatus.fromLedger("Absent", 0, 44));
        assertEquals(YearResultStatus.UNKNOWN, YearResultStatus.fromLedger("", 30, 44));
        assertEquals(YearResultStatus.UNKNOWN, YearResultStatus.fromLedger("Pass with grace", 44, 44));
    }

    @Test
    void missingResultLine() {
        // Verified all-clear encoding: Total-only trailer, full credits.
        assertEquals(YearResultStatus.ALL_CLEAR, YearResultStatus.fromLedger(null, 44, 44));
        // Anything else without a Result value: do not guess.
        assertEquals(YearResultStatus.UNKNOWN, YearResultStatus.fromLedger(null, 30, 44));
        assertEquals(YearResultStatus.UNKNOWN, YearResultStatus.fromLedger(null, 0, 44));
        assertEquals(YearResultStatus.UNKNOWN, YearResultStatus.fromLedger(null, null, null));
        assertEquals(YearResultStatus.UNKNOWN, YearResultStatus.fromLedger(null, 44, null));
    }

    @Test
    void normalizeOnlyForComparison() {
        assertEquals("fail a.t.k.t.", YearResultStatus.normalize("  Fail   A.T.K.T. "));
        assertNull(YearResultStatus.normalize(null));
    }
}
