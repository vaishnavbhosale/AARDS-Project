package com.aards.department;

import com.aards.common.SecurityUtil;
import com.aards.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Department APIs. Only admin can create.
@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController {

    private static final Logger log = LoggerFactory.getLogger(DepartmentController.class);

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DepartmentResponse>> create(@Valid @RequestBody DepartmentRequest request) {
        if (!SecurityUtil.hasRole("ADMIN")) {
            throw new org.springframework.security.access.AccessDeniedException("Only admin can create departments");
        }
        log.info("Create department request: {}", request.getCode());
        return ResponseEntity.ok(ApiResponse.success("Department created", departmentService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success("Departments fetched", departmentService.list()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DepartmentResponse>> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Department fetched", departmentService.getOne(id)));
    }
}
