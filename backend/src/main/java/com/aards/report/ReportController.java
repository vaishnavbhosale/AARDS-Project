package com.aards.report;

import com.aards.analytics.dto.AnalyticsFilterRequest;
import com.aards.common.SecurityUtil;
import com.aards.user.Role;
import com.aards.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// PDF downloads for faculty and above. Bytes go straight to the browser.
@RestController
@RequestMapping("/api/v1/reports")
@PreAuthorize("hasAnyRole('FACULTY', 'HOD', 'PRINCIPAL', 'ADMIN')")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/department")
    public ResponseEntity<byte[]> department(
            @RequestParam Long sessionId,
            @RequestParam Long departmentId,
            @RequestParam Integer year,
            @RequestParam Integer semester) {
        Long effectiveDeptId = hodScopedDepartmentId(departmentId);
        AnalyticsFilterRequest filter = AnalyticsFilterRequest.builder()
                .academicSessionId(sessionId)
                .departmentId(effectiveDeptId)
                .year(year)
                .semester(semester)
                .build();
        byte[] pdf = reportService.generateDepartmentReport(filter);
        log.info("Report generated: type={}, filters={}", "department", filter);
        return pdfResponse(pdf,
                "department-report-" + sessionId + "-" + effectiveDeptId + "-" + year + "-" + semester + ".pdf");
    }

    @GetMapping("/subject")
    public ResponseEntity<byte[]> subject(
            @RequestParam Long subjectId,
            @RequestParam Long sessionId,
            @RequestParam Long departmentId,
            @RequestParam Integer year,
            @RequestParam Integer semester) {
        Long effectiveDeptId = hodScopedDepartmentId(departmentId);
        AnalyticsFilterRequest filter = AnalyticsFilterRequest.builder()
                .academicSessionId(sessionId)
                .departmentId(effectiveDeptId)
                .year(year)
                .semester(semester)
                .build();
        byte[] pdf = reportService.generateSubjectReport(subjectId, filter);
        log.info("Report generated: type={}, filters={}", "subject", filter);
        return pdfResponse(pdf,
                "subject-report-" + subjectId + "-" + sessionId + "-" + effectiveDeptId + "-" + year + "-" + semester + ".pdf");
    }

    @GetMapping("/institute")
    public ResponseEntity<byte[]> institute(
            @RequestParam Long sessionId,
            @RequestParam Integer year,
            @RequestParam Integer semester) {
        User user = SecurityUtil.getCurrentUser();
        if (user.getRole() != Role.PRINCIPAL && user.getRole() != Role.ADMIN) {
            log.warn("User {} with role {} tried to access institute report",
                    user.getUsername(), user.getRole());
            throw new org.springframework.security.access.AccessDeniedException(
                    "Institute report is only available to Principal and Admin");
        }
        byte[] pdf = reportService.generateInstituteReport(sessionId, year, semester);
        log.info("Report generated: type={}, filters={}", "institute",
                "sessionId=" + sessionId + ", year=" + year + ", semester=" + semester);
        return pdfResponse(pdf,
                "institute-report-" + sessionId + "-" + year + "-" + semester + ".pdf");
    }

    // HODs always get their own department, whatever the query param says.
    private Long hodScopedDepartmentId(Long requestedDepartmentId) {
        User user = SecurityUtil.getCurrentUser();
        if (user.getRole() != Role.HOD) {
            return requestedDepartmentId;
        }
        if (user.getDepartmentId() == null) {
            throw new RuntimeException("HOD has no department assigned.");
        }
        if (!user.getDepartmentId().equals(requestedDepartmentId)) {
            log.warn("HOD {} asked for out-of-scope department report {}, using {}",
                    user.getUsername(), requestedDepartmentId, user.getDepartmentId());
        }
        return user.getDepartmentId();
    }

    // PDF bytes with download headers so the browser saves the file.
    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}
