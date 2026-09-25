package com.aards.analytics;

import com.aards.analytics.dto.AnalyticsFilterRequest;
import com.aards.analytics.dto.DashboardResponse;
import com.aards.common.SecurityUtil;
import com.aards.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Analytics APIs. Same dashboard logic as /api/v1/dashboard, kept for flexibility.
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>> dashboard(
            @RequestParam Long sessionId,
            @RequestParam Long departmentId,
            @RequestParam Integer year,
            @RequestParam Integer semester) {
        AnalyticsFilterRequest filter = AnalyticsFilterRequest.builder()
                .academicSessionId(sessionId)
                .departmentId(departmentId)
                .year(year)
                .semester(semester)
                .build();
        DashboardResponse response = analyticsService.getDashboard(filter, SecurityUtil.getCurrentUser());
        return ResponseEntity.ok(ApiResponse.success("Analytics fetched", response));
    }
}
