package com.aards.dashboard;

import com.aards.analytics.AnalyticsService;
import com.aards.analytics.dto.AnalyticsFilterRequest;
import com.aards.analytics.dto.DashboardResponse;
import com.aards.analytics.dto.FilterOptionsDto;
import com.aards.common.SecurityUtil;
import com.aards.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Dashboard APIs used by the frontend.
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final AnalyticsService analyticsService;

    public DashboardController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponse>> dashboard(
            @RequestParam Long sessionId,
            @RequestParam Long departmentId,
            @RequestParam Integer year,
            @RequestParam Integer semester) {
        log.info("Analytics generated for session={}, dept={}, year={}, sem={}",
                sessionId, departmentId, year, semester);
        AnalyticsFilterRequest filter = AnalyticsFilterRequest.builder()
                .academicSessionId(sessionId)
                .departmentId(departmentId)
                .year(year)
                .semester(semester)
                .build();
        DashboardResponse response = analyticsService.getDashboard(filter, SecurityUtil.getCurrentUser());
        return ResponseEntity.ok(ApiResponse.success("Dashboard fetched", response));
    }

    @GetMapping("/filters")
    public ResponseEntity<ApiResponse<FilterOptionsDto>> filters() {
        return ResponseEntity.ok(
                ApiResponse.success("Filter options fetched",
                        analyticsService.getFilterOptions(SecurityUtil.getCurrentUser())));
    }
}
