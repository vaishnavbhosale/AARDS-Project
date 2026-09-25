package com.aards.subject;

import com.aards.common.SecurityUtil;
import com.aards.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Subject APIs. Only admin can create, everyone logged in can list.
@RestController
@RequestMapping("/api/v1/subjects")
public class SubjectController {

    private static final Logger log = LoggerFactory.getLogger(SubjectController.class);

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SubjectResponse>> create(@Valid @RequestBody SubjectRequest request) {
        if (!SecurityUtil.hasRole("ADMIN")) {
            throw new org.springframework.security.access.AccessDeniedException("Only admin can create subjects");
        }
        log.info("Create subject request: {}", request.getCode());
        return ResponseEntity.ok(ApiResponse.success("Subject created", subjectService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> list(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer semester) {
        return ResponseEntity.ok(
                ApiResponse.success("Subjects fetched", subjectService.list(departmentId, year, semester)));
    }
}
