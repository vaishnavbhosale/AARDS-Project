package com.aards.auth.service;

import com.aards.auth.dto.LoginRequest;
import com.aards.auth.dto.LoginResponse;
import com.aards.security.JwtUtil;
import com.aards.user.Role;
import com.aards.user.User;
import com.aards.user.dto.RegisterRequest;
import com.aards.user.dto.UserResponse;
import com.aards.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Handles login and user creation. Passwords are always hashed with BCrypt.
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!user.isActive()) {
            throw new RuntimeException("Account is deactivated");
        }
        String token = jwtUtil.generateToken(user);
        log.info("Login successful for user: {}", user.getUsername());
        return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole())
                .departmentId(user.getDepartmentId())
                .build();
    }

    @Transactional
    public UserResponse register(RegisterRequest request, User currentUser) {
        log.info("Register request for username: {}", request.getUsername());

        // Bootstrap: if no users exist yet and first user is ADMIN, allow without login.
        boolean isBootstrap = userRepository.count() == 0 && request.getRole() == Role.ADMIN;
        if (!isBootstrap) {
            if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
                throw new RuntimeException("Only admin can create users");
            }
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already taken");
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already taken");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .email(request.getEmail())
                .role(request.getRole() == null ? Role.FACULTY : request.getRole())
                .departmentId(request.getDepartmentId())
                .active(true)
                .build();
        user = userRepository.save(user);
        log.info("User created: {} with role {}", user.getUsername(), user.getRole());
        return toResponse(user);
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
