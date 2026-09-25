package com.aards.security;

import com.aards.auth.dto.LoginRequest;
import com.aards.auth.service.AuthService;
import com.aards.user.Role;
import com.aards.user.User;
import com.aards.user.dto.RegisterRequest;
import com.aards.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Checks that admin APIs are locked: no token -> 401, faculty -> 403, admin -> 200.
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    private String facultyToken;
    private String adminToken;

    @BeforeEach
    void setupTokens() {
        User admin = userRepository.findByUsername("admin").orElseThrow();

        String suffix = String.valueOf(System.nanoTime() % 1000000);
        String facultyName = "fac_" + suffix;
        String adminName = "adm_" + suffix;

        authService.register(RegisterRequest.builder()
                .username(facultyName)
                .password("pass123")
                .fullName("Test Faculty")
                .email(facultyName + "@aards.local")
                .role(Role.FACULTY)
                .build(), admin);

        authService.register(RegisterRequest.builder()
                .username(adminName)
                .password("pass123")
                .fullName("Test Admin")
                .email(adminName + "@aards.local")
                .role(Role.ADMIN)
                .build(), admin);

        facultyToken = authService.login(LoginRequest.builder()
                .username(facultyName).password("pass123").build()).getToken();
        adminToken = authService.login(LoginRequest.builder()
                .username(adminName).password("pass123").build()).getToken();
    }

    @Test
    void noTokenGives401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void facultyTokenGives403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminTokenGives200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
