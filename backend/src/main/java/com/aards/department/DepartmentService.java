package com.aards.department;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Simple master data for departments.
@Service
public class DepartmentService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentService.class);

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        log.info("Creating department: {}", request.getCode());
        if (departmentRepository.findByCode(request.getCode()).isPresent()) {
            throw new RuntimeException("Department code already exists");
        }
        Department saved = departmentRepository.save(Department.builder()
                .name(request.getName())
                .code(request.getCode())
                .build());
        log.info("Department created with id: {}", saved.getId());
        return toResponse(saved);
    }

    public List<DepartmentResponse> list() {
        log.info("Listing departments");
        return departmentRepository.findAll().stream().map(this::toResponse).toList();
    }

    public DepartmentResponse getOne(Long id) {
        log.info("Fetching department id: {}", id);
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found: " + id));
        return toResponse(dept);
    }

    private DepartmentResponse toResponse(Department dept) {
        return DepartmentResponse.builder()
                .id(dept.getId())
                .name(dept.getName())
                .code(dept.getCode())
                .hodId(dept.getHodId())
                .build();
    }
}
