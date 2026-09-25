package com.aards.parser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Reads PDF text with PDFBox and extracts student rows with regex.
// TODO: Replace regex patterns when actual SPPU class result PDF format is received.
// For now we support a simple line-based format:
//   PRN: 1234567890 Name: Aarav Sharma Year: 2 Sem: 3
//   CS201 78/100
@Service
public class ParserService {

    private static final Logger log = LoggerFactory.getLogger(ParserService.class);

    // TODO: Replace regex patterns when actual SPPU class result PDF format is received.
    private static final String PRN_PATTERN = "PRN:\\s*(\\d{8,12})";
    private static final String NAME_PATTERN = "Name:\\s*([A-Za-z ]+)";
    private static final String YEAR_PATTERN = "Year:\\s*(\\d+)";
    private static final String SEM_PATTERN = "Sem:\\s*(\\d+)";
    private static final String ROLL_PATTERN = "Roll:\\s*(\\S+)";
    private static final String SUBJECT_PATTERN = "([A-Z]{2}\\d{3})";
    private static final String MARKS_PATTERN = "([A-Z]{2}\\d{3})\\s+(\\d+(?:\\.\\d+)?)/(\\d+(?:\\.\\d+)?)(?:\\s+([A-F][\\+\\-]?|O|AB))?";

    private static final Pattern PRN_REGEX = Pattern.compile(PRN_PATTERN);
    private static final Pattern NAME_REGEX = Pattern.compile(NAME_PATTERN);
    private static final Pattern YEAR_REGEX = Pattern.compile(YEAR_PATTERN);
    private static final Pattern SEM_REGEX = Pattern.compile(SEM_PATTERN);
    private static final Pattern ROLL_REGEX = Pattern.compile(ROLL_PATTERN);
    private static final Pattern MARKS_REGEX = Pattern.compile(MARKS_PATTERN);

    public List<ParsedRecord> parse(MultipartFile file) throws IOException {
        log.info("Parsing started for file: {}", file.getOriginalFilename());
        return parseBytes(file.getBytes(), file.getOriginalFilename());
    }

    // Same parsing but from raw bytes. Used when re-reading a saved file.
    public List<ParsedRecord> parseBytes(byte[] pdfBytes, String fileName) throws IOException {
        log.info("Parsing started for file: {}", fileName);
        try (PDDocument doc = PDDocument.load(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            log.info("PDF text extracted, length={}", text.length());
            List<ParsedRecord> records = parseText(text);
            log.info("Parsing completed: {} records", records.size());
            return records;
        } catch (IOException e) {
            log.error("Parsing failed", e);
            throw e;
        } catch (Exception e) {
            log.error("Parsing failed", e);
            throw new RuntimeException("Parsing failed: " + e.getMessage(), e);
        }
    }

    // Split text by student blocks. Each block starts with a PRN line.
    public List<ParsedRecord> parseText(String text) {
        List<ParsedRecord> records = new ArrayList<>();
        ParsedRecord current = null;

        for (String rawLine : text.split("\\r?\\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            Matcher prnMatcher = PRN_REGEX.matcher(line);
            if (prnMatcher.find()) {
                current = new ParsedRecord();
                current.setPrn(prnMatcher.group(1));
                current.setName(extractGroup(NAME_REGEX, line));
                current.setYear(parseIntOrDefault(extractGroup(YEAR_REGEX, line), 2));
                current.setSemester(parseIntOrDefault(extractGroup(SEM_REGEX, line), 3));
                String roll = extractGroup(ROLL_REGEX, line);
                current.setRollNumber(roll != null ? roll : current.getPrn());
                records.add(current);
                continue;
            }
            if (current != null) {
                Matcher markMatcher = MARKS_REGEX.matcher(line);
                if (markMatcher.find()) {
                    double obtained = Double.parseDouble(markMatcher.group(2));
                    double max = Double.parseDouble(markMatcher.group(3));
                    String grade = markMatcher.group(4);
                    SubjectMark mark = SubjectMark.builder()
                            .subjectCode(markMatcher.group(1))
                            .subjectName(markMatcher.group(1))
                            .marksObtained(obtained)
                            .maxMarks(max)
                            .grade(grade)
                            .status(obtained >= 0.4 * max ? "PASS" : "FAIL")
                            .build();
                    current.getMarks().add(mark);
                }
            }
        }
        return records;
    }

    private String extractGroup(Pattern pattern, String line) {
        Matcher m = pattern.matcher(line);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    private int parseIntOrDefault(String value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
