package com.aards.analytics;

import com.aards.analytics.dto.AnalyticsFilterRequest;
import com.aards.analytics.dto.DashboardResponse;
import com.aards.common.SecurityUtil;
import com.aards.common.dto.ApiResponse;
import com.aards.report.dto.SECombineReportResponse;
import com.aards.user.Role;
import com.aards.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Analytics APIs. Same dashboard logic as /api/v1/dashboard, kept for flexibility.
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    private final AnalyticsService analyticsService;
    private final SECombineReportService seCombineReportService;

    public AnalyticsController(AnalyticsService analyticsService,
                               SECombineReportService seCombineReportService) {
        this.analyticsService = analyticsService;
        this.seCombineReportService = seCombineReportService;
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

    @GetMapping("/se-combine")
    @PreAuthorize("hasAnyRole('FACULTY', 'HOD', 'PRINCIPAL', 'ADMIN')")
    public ResponseEntity<ApiResponse<SECombineReportResponse>> seCombine(
            @RequestParam Long sessionId,
            @RequestParam Long departmentId,
            @RequestParam Integer year,
            @RequestParam Integer semester) {
        // HODs stay inside their own department, like the dashboard.
        User user = SecurityUtil.getCurrentUser();
        Long effectiveDeptId = departmentId;
        if (user.getRole() == Role.HOD) {
            if (user.getDepartmentId() == null) {
                throw new RuntimeException("HOD has no department assigned.");
            }
            if (!user.getDepartmentId().equals(departmentId)) {
                log.warn("HOD {} asked for out-of-scope SE Combine department {}, using {}",
                        user.getUsername(), departmentId, user.getDepartmentId());
            }
            effectiveDeptId = user.getDepartmentId();
        }
        AnalyticsFilterRequest filter = AnalyticsFilterRequest.builder()
                .academicSessionId(sessionId)
                .departmentId(effectiveDeptId)
                .year(year)
                .semester(semester)
                .build();
        SECombineReportResponse response = seCombineReportService.generateSECombineReport(filter);
        log.info("SE Combine Report generated: session={}, dept={}, year={}",
                sessionId, effectiveDeptId, year);
        return ResponseEntity.ok(ApiResponse.success("SE Combine fetched", response));
    }
}
