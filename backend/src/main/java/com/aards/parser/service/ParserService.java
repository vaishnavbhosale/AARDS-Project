package com.aards.parser.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ParserService {

    private static final Logger log = LoggerFactory.getLogger(ParserService.class);

    public void parse(Long uploadId) {
        log.info("Parsing Started for uploadId={}", uploadId);
        // TODO Orchestrator: Check PDF type (Digital -> PDFBox, else OCR/Tess4J) -> Parser Agent.
        log.info("Parsing Completed for uploadId={}", uploadId);
    }
}
