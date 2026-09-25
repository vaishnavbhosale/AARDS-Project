package com.aards.parser;

import com.aards.parser.service.OcrService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Reads ledger text with PDFBox and extracts student blocks with regex.
// Parsing logic based on real SPPU College Ledger format (2019 Pattern). Update if format changes.
@Service
public class ParserService {

    private static final Logger log = LoggerFactory.getLogger(ParserService.class);

    private static final String PRN_LINE_PATTERN = "PRN:\\s*(\\d{8,9}[A-Z])\\s*Seat\\s*No\\.?:\\s*([A-Z0-9]+)\\s*NAME:\\s*(.+?)\\s*Mother\\s*-\\s*(.+)";
    private static final String SEMESTER_PATTERN = "SEMESTER:\\s*(\\d)";
    private static final String SUBJECT_ROW_PATTERN = "^(\\d{6})(?:[\\-_](\\d|PR|TW))?\\s+(.+)$";
    private static final String SGPA_PATTERN = "(First|Second)\\s+Semester\\s+SGPA\\s*:\\s*([0-9.]+|\\-\\-\\-)";
    private static final String CREDITS_PATTERN = "Credits\\s+Earned/Total\\s*:\\s*(\\d+)/(\\d+)";
    private static final String RESULT_PATTERN = "First Year (?:Result\\s*:\\s*(Pass|Fail)|Total\\s+Credits\\s+Earned)";
    private static final String GRADE_PATTERN = "\\b(O|A\\+|A|B\\+|B|C|P|F|FFF)\\b";
    private static final String TOTAL_POINTS_PATTERN = "Total Credit Points\\s*:\\s*(\\d+)";
    // First-page subject list: code, optional suffix, repeated code, title.
    // e.g. "101011- 1 PR 101011 Engineering Mechanics".
    private static final String SUBJECT_LIST_PATTERN =
            "^(\\d{6})(.*?)\\b\\1\\b\\s*(?:[-_]\\s*(?:\\d|PR|TW)\\s*|_\\s*(?:PR|TW)\\s*)*(.+)$";
    private static final String LIST_SEMESTER_PATTERN = "(?i)semester\\s*:\\s*(\\d+)";

    private static final Pattern PRN_LINE_REGEX = Pattern.compile(PRN_LINE_PATTERN);
    private static final Pattern SEMESTER_REGEX = Pattern.compile(SEMESTER_PATTERN);
    private static final Pattern SUBJECT_ROW_REGEX = Pattern.compile(SUBJECT_ROW_PATTERN);
    private static final Pattern SGPA_REGEX = Pattern.compile(SGPA_PATTERN);
    private static final Pattern CREDITS_REGEX = Pattern.compile(CREDITS_PATTERN);
    private static final Pattern RESULT_REGEX = Pattern.compile(RESULT_PATTERN);
    private static final Pattern GRADE_REGEX = Pattern.compile("^" + GRADE_PATTERN + "$");
    private static final Pattern TOTAL_POINTS_REGEX = Pattern.compile(TOTAL_POINTS_PATTERN);
    private static final Pattern SUBJECT_LIST_REGEX = Pattern.compile(SUBJECT_LIST_PATTERN);
    private static final Pattern LIST_SEMESTER_REGEX = Pattern.compile(LIST_SEMESTER_PATTERN);

    private final OcrService ocrService;

    @Value("${aards.ocr.enabled:false}")
    private boolean ocrEnabled;

    public ParserService(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    public List<ParsedRecord> parse(MultipartFile file) throws IOException {
        log.info("Parsing started for file: {}", file.getOriginalFilename());
        return parseBytes(file.getBytes(), file.getOriginalFilename(), file);
    }

    // Same parsing but from raw bytes. Used when re-reading a saved file.
    public List<ParsedRecord> parseBytes(byte[] pdfBytes, String fileName) throws IOException {
        return parseBytes(pdfBytes, fileName, null);
    }

    private List<ParsedRecord> parseBytes(byte[] pdfBytes, String fileName, MultipartFile file) throws IOException {
        log.info("Parsing started for file: {}", fileName);
        String text;
        try (PDDocument doc = PDDocument.load(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            text = stripper.getText(doc);
            log.info("PDF text extracted, length={}", text == null ? 0 : text.length());
        } catch (IOException e) {
            log.error("Parsing failed", e);
            throw e;
        } catch (Exception e) {
            log.error("Parsing failed", e);
            throw new RuntimeException("Parsing failed: " + e.getMessage(), e);
        }
        if (isScanned(text) && file != null && ocrEnabled) {
            log.info("PDF looks scanned, using OCR fallback");
            String ocrText = ocrService.extractText(file);
            if (ocrText != null && !ocrText.isBlank()) {
                text = ocrText;
            }
        }
        List<ParsedRecord> records = parseText(text);
        log.info("Parsing completed: {} records", records.size());
        return records;
    }

    private boolean isScanned(String text) {
        if (text == null || !text.contains("PRN:")) {
            return true;
        }
        return text.split("\\r?\\n").length < 5;
    }

    // Split text by student blocks. Each block starts with a PRN line.
    // Lines that match nothing are skipped silently.
    public List<ParsedRecord> parseText(String text) {
        List<ParsedRecord> records = new ArrayList<>();
        if (text == null) {
            return records;
        }
        ParsedRecord current = null;
        // Semester section we are currently reading. Reset for each student.
        Integer currentSemester = null;

        for (String rawLine : text.split("\\r?\\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            Matcher prnMatcher = PRN_LINE_REGEX.matcher(line);
            if (prnMatcher.find()) {
                current = ParsedRecord.builder()
                        .prn(prnMatcher.group(1).trim())
                        .rollNumber(prnMatcher.group(2).trim())
                        .seatNumber(prnMatcher.group(2).trim())
                        .name(prnMatcher.group(3).trim())
                        .motherName(prnMatcher.group(4).trim())
                        .year(1)
                        .semester(1)
                        .overallResult("UNKNOWN")
                        .build();
                currentSemester = null;
                records.add(current);
                continue;
            }
            if (current == null) {
                continue;
            }
            Matcher semMatcher = SEMESTER_REGEX.matcher(line);
            if (semMatcher.find()) {
                currentSemester = parseIntOrDefault(semMatcher.group(1),
                        currentSemester == null ? current.getSemester() : currentSemester);
                continue;
            }
            SubjectMark mark = tryParseSubjectRow(line);
            if (mark != null) {
                // Tag the row itself. The record's top-level semester is left alone.
                mark.setSemester(currentSemester);
                current.getMarks().add(mark);
                continue;
            }
            Matcher sgpaMatcher = SGPA_REGEX.matcher(line);
            if (sgpaMatcher.find()) {
                current.getSemesters().add(SemesterSummary.builder()
                        .semester(currentSemester == null ? current.getSemester() : currentSemester)
                        .sgpa(parseDoubleOrNull(sgpaMatcher.group(2)))
                        .creditsEarned(parseIntOrNull(extractGroup(CREDITS_REGEX, line, 1)))
                        .totalCredits(parseIntOrNull(extractGroup(CREDITS_REGEX, line, 2)))
                        .totalCreditPoints(parseIntOrNull(extractGroup(TOTAL_POINTS_REGEX, line, 1)))
                        .build());
                continue;
            }
            Matcher resultMatcher = RESULT_REGEX.matcher(line);
            if (resultMatcher.find()) {
                String outcome = resultMatcher.group(1);
                current.setOverallResult(
                        outcome == null || outcome.equalsIgnoreCase("Pass") ? "PASS" : "FAIL");
            }
        }
        return records;
    }

    // Raw PDF text for helpers (e.g. the subject-name list). Plain PDFBox,
    // no OCR. Used by UploadService alongside parse().
    public String extractFullText(byte[] pdfBytes) throws IOException {
        try (PDDocument doc = PDDocument.load(pdfBytes)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    // Subject list from the ledger's first page: code ("101011-1") -> title
    // ("Engineering Mechanics"). Header and non-matching lines are skipped.
    public Map<String, String> extractSubjectNames(String fullText) {
        Map<String, String> names = new LinkedHashMap<>();
        if (fullText == null) {
            return names;
        }
        Integer listSemester = null;
        for (String rawLine : fullText.split("\\r?\\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            Matcher semMatcher = LIST_SEMESTER_REGEX.matcher(line);
            if (semMatcher.find()) {
                listSemester = parseIntOrNull(semMatcher.group(1));
                continue;
            }
            String lower = line.toLowerCase();
            if (lower.contains("code") && lower.contains("paper") && lower.contains("title")) {
                continue;
            }
            Matcher m = SUBJECT_LIST_REGEX.matcher(line);
            if (!m.matches()) {
                continue;
            }
            String title = m.group(3).trim();
            if (title.isEmpty()) {
                continue;
            }
            String key = m.group(1) + normalizeListSuffix(m.group(2));
            names.putIfAbsent(key, title);
        }
        log.info("Extracted {} subject names from list page (semester {})",
                names.size(), listSemester);
        return names;
    }

    // Suffix junk between the two codes, canonicalized: "- 1" -> "-1",
    // "- 1 PR" -> "-1_PR", "_TW" stays "_TW", "" stays "".
    private String normalizeListSuffix(String middle) {
        String s = middle.replaceAll("\\s+", "").toUpperCase();
        if (s.isEmpty()) {
            return "";
        }
        Matcher m = Pattern.compile("^(-\\d+)(PR|TW)$").matcher(s);
        if (m.matches()) {
            return m.group(1) + "_" + m.group(2);
        }
        return s;
    }

    // A subject row starts with a 6-digit code, e.g. "101011- 1 P 014 ..." or "101011- 1_ PR --- ...".
    private SubjectMark tryParseSubjectRow(String line) {
        // Join a spaced dash ("101011- 1 ..." -> "101011-1 ...") so the pattern matches.
        String normalized = line.replaceFirst("^(\\d{6})\\s*-\\s*", "$1-");
        Matcher m = SUBJECT_ROW_REGEX.matcher(normalized);
        String code;
        String suffix = "";
        String rest;
        if (m.find()) {
            code = m.group(1);
            if (m.group(2) != null) {
                suffix = "-" + m.group(2);
            }
            rest = m.group(3).trim();
            // Compound suffix like "_PR" in "101011-1_PR ..." is not part of the pattern.
            Matcher extra = Pattern.compile("^_(PR|TW)\\b").matcher(rest);
            if (extra.find()) {
                suffix += "_" + extra.group(1);
                rest = rest.substring(extra.end()).trim();
            }
        } else {
            // Odd separators: parse the head manually, e.g. "101011-1_ PR --- ...".
            Matcher g = Pattern.compile("^(\\d{6})\\b\\s*(.*)$").matcher(normalized);
            if (!g.find()) {
                return null;
            }
            code = g.group(1);
            String tail = g.group(2).trim();
            if (tail.startsWith("-")) {
                tail = tail.substring(1).trim();
            }
            String[] head = tail.split("\\s+");
            int used = 0;
            if (head.length > 0 && head[0].matches("\\d+(_(PR|TW))?")) {
                suffix = "-" + head[0];
                used = 1;
            } else if (head.length > 1 && head[0].matches("\\d+_")
                    && head[1].matches("(PR|TW)")) {
                // Split suffix like "1_ PR" -> "-1_PR".
                suffix = "-" + head[0].replace("_", "") + "_" + head[1];
                used = 2;
            } else if (head.length > 0 && head[0].matches("(PR|TW)")) {
                suffix = "_" + head[0];
                used = 1;
            } else {
                return null;
            }
            rest = String.join(" ", Arrays.copyOfRange(head, used, head.length)).trim();
        }

        // Tail columns: Total, Credits, CreditsEarned, Grade, GradePoints, CreditPoints.
        // An extra FFF marker can sit between Total and Credits on fail rows.
        // Practical rows use digit "0" as grade (really letter O = Outstanding).
        Matcher tail = Pattern.compile(
                "(\\d+)\\s+(?:FFF\\s+)?(\\d+)\\s+(\\d+)\\s+(O|0|A\\+|A|B\\+|B|C|P|F|FFF)\\s+(\\d+)\\s+(\\d+)$")
                .matcher(rest);
        if (!tail.find()) {
            // No tail (e.g. absent row with "AC"): skip marks, mark absent.
            return SubjectMark.builder()
                    .subjectCode(code + suffix)
                    .subjectSuffix(suffix)
                    .subjectName(code + suffix)
                    .marksObtained(0.0)
                    .maxMarks(100.0)
                    .grade(null)
                    .status("ABSENT")
                    .build();
        }
        String grade = tail.group(4);
        if ("0".equals(grade)) {
            grade = "O";
        }
        String status = "PASS";
        if ("F".equals(grade) || "FFF".equals(grade)) {
            status = "FAIL";
        } else {
            for (String token : rest.split("\\s+")) {
                if ("AC".equals(token)) {
                    status = "ABSENT";
                    break;
                }
            }
        }
        return SubjectMark.builder()
                .subjectCode(code + suffix)
                .subjectSuffix(suffix)
                .subjectName(code + suffix)
                .marksObtained(Double.parseDouble(tail.group(1)))
                .maxMarks(100.0)
                .grade(grade)
                .status(status)
                .build();
    }

    private String extractGroup(Pattern pattern, String line, int group) {
        Matcher m = pattern.matcher(line);
        if (m.find()) {
            return m.group(group);
        }
        return null;
    }

    private Integer parseIntOrNull(String value) {
        try {
            return value == null ? null : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseDoubleOrNull(String value) {
        if (value == null || value.contains("-")) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int parseIntOrDefault(String value, int fallback) {
        Integer parsed = parseIntOrNull(value);
        return parsed == null ? fallback : parsed;
    }
}
