package com.aards.validation.controller;

import com.aards.common.dto.ApiResponse;
import com.aards.validation.service.ValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/validation")
public class ValidationController {

    private final ValidationService validationService;

    public ValidationController(ValidationService validationService) {
        this.validationService = validationService;
    }

    @GetMapping("/{uploadId}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> errors(@PathVariable Long uploadId) {
        return ResponseEntity.ok(ApiResponse.success("Validation errors fetched",
                validationService.getErrors(uploadId)));
    }

    @PostMapping("/{uploadId}/approve")
    public ResponseEntity<ApiResponse<Void>> approve(@PathVariable Long uploadId) {
        validationService.approve(uploadId);
        return ResponseEntity.ok(ApiResponse.success("Validation approved", null));
    }
}
