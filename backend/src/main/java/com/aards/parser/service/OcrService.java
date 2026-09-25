package com.aards.parser.service;

import net.sourceforge.tess4j.Tesseract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;

// Basic Tesseract wiring for scanned PDFs. Returns empty text if OCR is unavailable.
@Service
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    public String extractText(MultipartFile file) {
        log.info("OCR started for file: {}", file.getOriginalFilename());
        try {
            File temp = File.createTempFile("aards-ocr-", ".pdf");
            try {
                Files.write(temp.toPath(), file.getBytes());
                Tesseract tesseract = new Tesseract();
                tesseract.setDatapath("tessdata");
                tesseract.setLanguage("eng");
                String text = tesseract.doOCR(temp);
                log.info("OCR completed, length={}", text == null ? 0 : text.length());
                return text == null ? "" : text;
            } finally {
                Files.deleteIfExists(temp.toPath());
            }
        } catch (Exception e) {
            log.warn("Tesseract OCR is not available ({}). Skipping OCR.", e.getMessage());
            return "";
        }
    }
}
