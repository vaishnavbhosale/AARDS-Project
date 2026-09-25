package com.aards.upload.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UploadService {

    private static final Logger log = LoggerFactory.getLogger(UploadService.class);

    public Long handleUpload(String filename) {
        log.info("Upload Started: {}", filename);
        // TODO: store original PDF, trigger Orchestrator -> Parser -> Validation chain.
        log.info("Upload Completed: {}", filename);
        return 1L;
    }
}
