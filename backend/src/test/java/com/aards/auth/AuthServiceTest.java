package com.aards.auth;

import com.aards.auth.dto.LoginRequest;
import com.aards.auth.dto.LoginResponse;
import com.aards.auth.service.AuthService;
import com.aards.user.Role;
import com.aards.user.User;
import com.aards.user.dto.RegisterRequest;
import com.aards.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Tests register + login flow.
@SpringBootTest
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void registerAndLogin() {
        User admin = userRepository.findByUsername("admin").orElseThrow();

        RegisterRequest request = RegisterRequest.builder()
                .username("faculty1")
                .password("pass123")
                .fullName("Faculty One")
                .email("faculty1@aards.local")
                .role(Role.FACULTY)
                .build();
        authService.register(request, admin);

        LoginResponse response = authService.login(LoginRequest.builder()
                .username("faculty1")
                .password("pass123")
                .build());

        assertNotNull(response.getToken());
    }

    @Test
    void loginWithWrongPasswordFails() {
        User admin = userRepository.findByUsername("admin").orElseThrow();

        authService.register(RegisterRequest.builder()
                .username("faculty2")
                .password("pass123")
                .fullName("Faculty Two")
                .email("faculty2@aards.local")
                .role(Role.FACULTY)
                .build(), admin);

        assertThrows(Exception.class, () ->
                authService.login(LoginRequest.builder()
                        .username("faculty2")
                        .password("wrongpass")
                        .build()));
    }
}
