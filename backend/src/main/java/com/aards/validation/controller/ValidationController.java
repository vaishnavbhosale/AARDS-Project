package com.aards.validation.controller;

import com.aards.common.dto.ApiResponse;
import com.aards.validation.ValidationError;
import com.aards.validation.ValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Shows doubtful values so the teacher can fix them.
@RestController
@RequestMapping("/api/v1/validation")
public class ValidationController {

    private final ValidationService validationService;

    public ValidationController(ValidationService validationService) {
        this.validationService = validationService;
    }

    @GetMapping("/{uploadId}")
    public ResponseEntity<ApiResponse<List<ValidationError>>> errors(@PathVariable Long uploadId) {
        return ResponseEntity.ok(
                ApiResponse.success("Validation errors fetched", validationService.getErrors(uploadId)));
    }
}
