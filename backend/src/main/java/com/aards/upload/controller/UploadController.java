package com.aards.upload.controller;

import com.aards.common.dto.ApiResponse;
import com.aards.upload.service.UploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/uploads")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> upload(@RequestParam("file") MultipartFile file) {
        Long id = uploadService.handleUpload(file.getOriginalFilename());
        return ResponseEntity.ok(ApiResponse.success("Upload completed successfully",
                Map.of("uploadId", id, "filename", String.valueOf(file.getOriginalFilename()))));
    }
}
