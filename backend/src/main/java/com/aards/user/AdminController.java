package com.aards.user;

import com.aards.common.SecurityUtil;
import com.aards.common.dto.ApiResponse;
import com.aards.user.dto.UserResponse;
import com.aards.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Admin-only user management. Role is checked manually to keep it simple.
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
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
