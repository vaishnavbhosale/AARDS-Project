package com.aards.auth.service;

import com.aards.auth.dto.LoginRequest;
import com.aards.auth.dto.LoginResponse;
import com.aards.security.JwtUtil;
import com.aards.user.Role;
import com.aards.user.dto.UserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final JwtUtil jwtUtil;

    public AuthService(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt started for username={}", request.getUsername());
        // TODO V1: load user from DB, verify password with PasswordEncoder, check enabled flag.
        // Placeholder token for skeleton so frontend auth flow can be wired.
        String token = jwtUtil.generateToken(request.getUsername(), Role.FACULTY.name());
        UserDto user = UserDto.builder()
                .id(1L)
                .username(request.getUsername())
                .role(Role.FACULTY)
                .fullName("Placeholder Faculty")
                .build();
        log.info("Login completed for username={}", request.getUsername());
        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(user)
                .build();
    }
}
