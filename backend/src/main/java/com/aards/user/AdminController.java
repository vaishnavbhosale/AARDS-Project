package com.aards.user;

import com.aards.common.SecurityUtil;
import com.aards.common.dto.ApiResponse;
import com.aards.subject.SubjectFacultyRequest;
import com.aards.subject.SubjectFacultyResponse;
import com.aards.subject.SubjectFacultyService;
import com.aards.user.dto.UserResponse;
import com.aards.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Admin-only user management. Role is checked manually to keep it simple.
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final UserRepository userRepository;
    private final SubjectFacultyService subjectFacultyService;

    public AdminController(UserRepository userRepository, SubjectFacultyService subjectFacultyService) {
        this.userRepository = userRepository;
        this.subjectFacultyService = subjectFacultyService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> list() {
        checkAdmin();
        log.info("Admin listing users");
        List<UserResponse> users = userRepository.findAll().stream().map(this::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success("Users fetched", users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getOne(@PathVariable Long id) {
        checkAdmin();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
        return ResponseEntity.ok(ApiResponse.success("User fetched", toResponse(user)));
    }

    @PutMapping("/{id}/deactivate")
    @Transactional
    public ResponseEntity<ApiResponse<UserResponse>> deactivate(@PathVariable Long id) {
        checkAdmin();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
        user.setActive(false);
        userRepository.save(user);
        log.info("User deactivated: {}", user.getUsername());
        return ResponseEntity.ok(ApiResponse.success("User deactivated", toResponse(user)));
    }

    @PutMapping("/{id}/activate")
    @Transactional
    public ResponseEntity<ApiResponse<UserResponse>> activate(@PathVariable Long id) {
        checkAdmin();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
        user.setActive(true);
        userRepository.save(user);
        log.info("User activated: {}", user.getUsername());
        return ResponseEntity.ok(ApiResponse.success("User activated", toResponse(user)));
    }

    private void checkAdmin() {
        if (!SecurityUtil.hasRole("ADMIN")) {
            throw new org.springframework.security.access.AccessDeniedException("Only admin can do this");
        }
    }

    @PostMapping("/subject-faculty")
    public ResponseEntity<ApiResponse<SubjectFacultyResponse>> assignFaculty(
            @RequestBody SubjectFacultyRequest request) {
        checkAdmin();
        SubjectFacultyResponse response = subjectFacultyService.assign(request);
        log.info("Faculty assigned: subject {} to user {}",
                request.getSubjectId(), request.getFacultyId());
        return ResponseEntity.ok(ApiResponse.success("Faculty assigned", response));
    }

    @GetMapping("/subject-faculty")
    public ResponseEntity<ApiResponse<List<SubjectFacultyResponse>>> listFacultyAssignments(
            @RequestParam Long academicSessionId) {
        checkAdmin();
        List<SubjectFacultyResponse> assignments = subjectFacultyService.list(academicSessionId);
        return ResponseEntity.ok(ApiResponse.success("Assignments fetched", assignments));
    }

    @DeleteMapping("/subject-faculty/{id}")
    public ResponseEntity<ApiResponse<String>> deleteFacultyAssignment(@PathVariable Long id) {
        checkAdmin();
        subjectFacultyService.delete(id);
        log.info("Faculty assignment deleted: {}", id);
        return ResponseEntity.ok(ApiResponse.success("Assignment deleted", "deleted"));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .departmentId(user.getDepartmentId())
                .active(user.isActive())
                .build();
    }
}
