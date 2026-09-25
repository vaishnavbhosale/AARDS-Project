package com.aards.validation.controller;

import com.aards.common.SecurityUtil;
import com.aards.common.dto.ApiResponse;
import com.aards.upload.UploadBatchResponse;
import com.aards.upload.UploadService;
import com.aards.user.User;
import com.aards.validation.ValidationErrorResponse;
import com.aards.validation.ValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// Teacher fixes doubtful values here, then approves the batch.
@RestController
@RequestMapping("/api/v1/validation")
public class ValidationController {

    private final ValidationService validationService;
    private final UploadService uploadService;

    public ValidationController(ValidationService validationService, UploadService uploadService) {
        this.validationService = validationService;
        this.uploadService = uploadService;
    }

    @GetMapping("/{uploadId}")
    public ResponseEntity<ApiResponse<List<ValidationErrorResponse>>> errors(@PathVariable Long uploadId) {
        return ResponseEntity.ok(
                ApiResponse.success("Validation errors fetched", validationService.getErrors(uploadId)));
    }

    @GetMapping("/batch/{batchId}")
    public ResponseEntity<ApiResponse<List<ValidationErrorResponse>>> errorsByBatch(@PathVariable Long batchId) {
        return ResponseEntity.ok(
                ApiResponse.success("Validation errors fetched", validationService.getErrors(batchId)));
    }

    @PutMapping("/{errorId}")
    public ResponseEntity<ApiResponse<ValidationErrorResponse>> update(
            @PathVariable Long errorId, @RequestBody Map<String, String> body) {
        checkFaculty();
        User reviewer = SecurityUtil.getCurrentUser();
        ValidationErrorResponse response =
                validationService.updateValidationError(errorId, body.get("correctedValue"), reviewer);
        return ResponseEntity.ok(ApiResponse.success("Validation error updated", response));
    }

    @PostMapping("/batch/{batchId}/approve")
    public ResponseEntity<ApiResponse<UploadBatchResponse>> approve(@PathVariable Long batchId) {
        checkFaculty();
        User reviewer = SecurityUtil.getCurrentUser();
        validationService.approveBatch(batchId, reviewer);
        return ResponseEntity.ok(
                ApiResponse.success("Batch approved", uploadService.getById(batchId)));
    }

    private void checkFaculty() {
        if (!SecurityUtil.hasRole("FACULTY") && !SecurityUtil.hasRole("ADMIN")) {
            throw new org.springframework.security.access.AccessDeniedException("Only faculty can do this");
        }
    }
}
