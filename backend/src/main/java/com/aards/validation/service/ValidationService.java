package com.aards.validation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ValidationService {

    private static final Logger log = LoggerFactory.getLogger(ValidationService.class);

    public List<Map<String, Object>> getErrors(Long uploadId) {
        log.info("Validation Started for uploadId={}", uploadId);
        // TODO: Validation Agent -> return list of {student, field, extractedValue, correctValue}.
        return List.of();
    }

    public void approve(Long uploadId) {
        log.info("Validation Approved for uploadId={}", uploadId);
    }
}
