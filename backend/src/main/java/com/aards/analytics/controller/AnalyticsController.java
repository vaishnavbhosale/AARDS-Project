package com.aards.analytics.controller;

import com.aards.analytics.service.AnalyticsService;
import com.aards.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> get(@RequestParam(defaultValue = "1") Long uploadId) {
        return ResponseEntity.ok(ApiResponse.success("Analytics fetched", analyticsService.generate(uploadId)));
    }
}
