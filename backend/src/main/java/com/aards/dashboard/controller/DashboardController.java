package com.aards.dashboard.controller;

import com.aards.common.dto.ApiResponse;
import com.aards.dashboard.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> get(
            @RequestParam(required = false) String session,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String department) {
        return ResponseEntity.ok(ApiResponse.success("Dashboard fetched",
                dashboardService.getDashboard(session, year, semester, department)));
    }
}
