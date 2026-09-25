package com.aards.report.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    public String generate(String type, Long uploadId) {
        log.info("Report generation started: type={} uploadId={}", type, uploadId);
        // TODO: PDF-only reports: institute, department, subject, SE/TE/BE.
        log.info("Report Generated: type={} uploadId={}", type, uploadId);
        return type + "-" + uploadId + ".pdf";
    }
}
