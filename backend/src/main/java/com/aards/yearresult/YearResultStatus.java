package com.aards.yearresult;

/**
 * Normalized form of the university's official year result
 * ({@code SECOND YEAR Result : ...} on the SPPU ledger).
 *
 * <p>Verified against the real SE NEP 2020 ledger (PDFBox extraction):
 * <ul>
 *   <li>All clear: no {@code Result :} value at all, only
 *       {@code SECOND YEAR Total Credits Earned : 44/44} (69 students).</li>
 *   <li>ATKT: {@code Fail A.T.K.T.} (67 students).</li>
 *   <li>Fail: {@code Fail} (20 students).</li>
 *   <li>Absent: no student-level string exists in this ledger.</li>
 * </ul>
 * Anything else stays {@link #UNKNOWN}: never guessed, reported by callers.
 */
public enum YearResultStatus {
    ALL_CLEAR,
    ATKT,
    FAIL,
    ABSENT,
    UNKNOWN;

    /**
     * Maps the raw ledger value to a status. Comparison is
     * case-insensitive with collapsed whitespace; the raw value itself is
     * never modified. {@code null} raw (no Result line) is ALL_CLEAR only
     * for the verified all-clear encoding (full year credits earned),
     * otherwise UNKNOWN.
     *
     * @param rawValue official value after {@code Result :}, {@code null} when absent
     * @param creditsEarned year credits earned from the credit trailer, nullable
     * @param totalCredits year total credits from the credit trailer, nullable
     */
    public static YearResultStatus fromLedger(String rawValue, Integer creditsEarned, Integer totalCredits) {
        if (rawValue != null) {
            String normalized = normalize(rawValue);
            if (normalized.equals("pass")) {
                return ALL_CLEAR;
            }
            if (normalized.equals("fail")) {
                return FAIL;
            }
            // Verified ledger string is "Fail A.T.K.T."; accept dot/space variants.
            String compact = normalized.replace(".", "").replace(" ", "");
            if (compact.equals("failatkt")) {
                return ATKT;
            }
            return UNKNOWN;
        }
        if (creditsEarned != null && totalCredits != null && totalCredits > 0
                && creditsEarned.intValue() == totalCredits.intValue()) {
            return ALL_CLEAR;
        }
        return UNKNOWN;
    }

    /** Trimmed, single-spaced, lower-case form used only for comparison. */
    public static String normalize(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        return rawValue.trim().replaceAll("\\s+", " ").toLowerCase(java.util.Locale.ROOT);
    }
}
