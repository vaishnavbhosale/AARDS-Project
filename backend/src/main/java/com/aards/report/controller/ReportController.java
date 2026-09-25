package com.aards.report.controller;

import com.aards.common.dto.ApiResponse;
import com.aards.report.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> generate(
            @RequestParam(defaultValue = "institute") String type,
            @RequestParam(defaultValue = "1") Long uploadId) {
        String file = reportService.generate(type, uploadId);
        return ResponseEntity.ok(ApiResponse.success("Report generated", Map.of("file", file)));
    }
}
