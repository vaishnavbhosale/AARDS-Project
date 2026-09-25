package com.aards.parser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Reads PDF text with PDFBox and extracts student rows with regex.
// TODO: Update regex when actual SPPU format is received.
@Service
public class ParserService {

    private static final Logger log = LoggerFactory.getLogger(ParserService.class);

    // Simple table format we support for now:
    // PRN: 12345678 Name: Aarav Sharma Year: 2 Sem: 3
    // CS201 75/100 A
    private static final String PRN_REGEX = "PRN:\\s*(\\d+)";
    private static final String NAME_REGEX = "Name:\\s*([A-Za-z ]+)";
    private static final String YEAR_REGEX = "Year:\\s*(\\d+)";
    private static final String SEM_REGEX = "Sem:\\s*(\\d+)";
    private static final String ROLL_REGEX = "Roll:\\s*(\\S+)";
    private static final String MARK_REGEX = "([A-Z]{2}\\d{3})\\s+(\\d+(?:\\.\\d+)?)/(\\d+(?:\\.\\d+)?)\\s+([A-F][\\+\\-]?|O)";

    private static final Pattern PRN_PATTERN = Pattern.compile(PRN_REGEX);
    private static final Pattern NAME_PATTERN = Pattern.compile(NAME_REGEX);
    private static final Pattern YEAR_PATTERN = Pattern.compile(YEAR_REGEX);
    private static final Pattern SEM_PATTERN = Pattern.compile(SEM_REGEX);
    private static final Pattern ROLL_PATTERN = Pattern.compile(ROLL_REGEX);
    private static final Pattern MARK_PATTERN = Pattern.compile(MARK_REGEX);

    public List<ParsedRecord> parse(MultipartFile file) {
        log.info("Parsing started for file: {}", file.getOriginalFilename());
        try (PDDocument doc = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            log.info("PDF text extracted, length={}", text.length());
            List<ParsedRecord> records = parseText(text);
            log.info("Parsing completed, records={}", records.size());
            return records;
        } catch (Exception e) {
            log.error("Parsing failed", e);
            throw new RuntimeException("Parsing failed: " + e.getMessage(), e);
        }
    }

    // Split text into student blocks. Each block starts with a PRN line.
    public List<ParsedRecord> parseText(String text) {
        List<ParsedRecord> records = new ArrayList<>();
        ParsedRecord current = null;

        for (String rawLine : text.split("\\r?\\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            Matcher prnMatcher = PRN_PATTERN.matcher(line);
            if (prnMatcher.find()) {
                // New student starts here
                current = new ParsedRecord();
                current.setPrn(prnMatcher.group(1));
                current.setName(extractGroup(NAME_PATTERN, line));
                current.setYear(parseIntOrDefault(extractGroup(YEAR_PATTERN, line), 2));
                current.setSemester(parseIntOrDefault(extractGroup(SEM_PATTERN, line), 3));
                String roll = extractGroup(ROLL_PATTERN, line);
                current.setRollNumber(roll != null ? roll : current.getPrn());
                records.add(current);
                continue;
            }
            if (current != null) {
                Matcher markMatcher = MARK_PATTERN.matcher(line);
                if (markMatcher.find()) {
                    SubjectMark mark = new SubjectMark();
                    mark.setSubjectCode(markMatcher.group(1));
                    mark.setMarks(Double.parseDouble(markMatcher.group(2)));
                    mark.setMaxMarks(Double.parseDouble(markMatcher.group(3)));
                    mark.setGrade(markMatcher.group(4));
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
