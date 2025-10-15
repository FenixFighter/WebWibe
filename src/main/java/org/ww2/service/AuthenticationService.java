package org.ww2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.ww2.dto.AuthRequest;
import org.ww2.dto.AuthResponse;
import org.ww2.entity.User;
import org.ww2.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final AuthService authService;
    private final UserRepository userRepository;

    /**
     * Handles user login
     */
    public AuthResponse handleLogin(AuthRequest request) {
        try {
            AuthResponse response = authService.authenticate(request);
            log.info("User {} logged in successfully", request.getUsername());
            return response;
        } catch (Exception e) {
            log.warn("Login failed for user: {}", request.getUsername());
            return new AuthResponse(null, null, null, null);
        }
    }

    /**
     * Handles user logout
     */
    public void handleLogout(String token) {
        try {
            authService.logout(token);
            log.info("User logged out successfully");
        } catch (Exception e) {
            log.error("Logout error", e);
            throw new RuntimeException("Logout failed", e);
        }
    }

    /**
     * Handles user registration
     */
    public User handleRegistration(String username, String password, String email, User.UserRole role) {
        try {
            // Check if user already exists
            if (userRepository.findByUsername(username).isPresent()) {
                throw new RuntimeException("Username already exists");
            }

            User user = authService.createUser(username, password, email, role);
            log.info("User {} registered successfully with role {}", username, role);
            return user;
        } catch (Exception e) {
            log.error("Registration error for user: {}", username, e);
            throw new RuntimeException("Registration failed", e);
        }
    }

    /**
     * Validates registration request
     */
    public void validateRegistrationRequest(String username, String password, String email) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (username.length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters long");
        }
        if (password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long");
        }
    }
}
