package org.ww2.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.ww2.dto.AuthRequest;
import org.ww2.dto.AuthResponse;
import org.ww2.entity.User;
import org.ww2.service.AuthenticationService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            AuthResponse response = authenticationService.handleLogin(request);
            if (response.getToken() != null) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(401).body(response);
            }
        } catch (Exception e) {
            log.error("Authentication failed for user: {}", request.getUsername(), e);
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String token) {
        try {
            authenticationService.handleLogout(token.replace("Bearer ", ""));
            return ResponseEntity.ok("Logged out successfully");
        } catch (Exception e) {
            log.error("Logout failed", e);
            return ResponseEntity.status(400).body("Logout failed");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            authenticationService.validateRegistrationRequest(
                request.getUsername(), 
                request.getPassword(), 
                request.getEmail()
            );

            authenticationService.handleRegistration(
                request.getUsername(),
                request.getPassword(),
                request.getEmail(),
                User.UserRole.CUSTOMER
            );
            return ResponseEntity.ok("User created successfully");
        } catch (IllegalArgumentException e) {
            log.warn("Registration validation failed: {}", e.getMessage());
            return ResponseEntity.status(400).body(e.getMessage());
        } catch (Exception e) {
            log.error("Registration failed", e);
            return ResponseEntity.status(400).body("Registration failed");
        }
    }

    @PostMapping("/register-support")
    public ResponseEntity<?> registerSupport(@RequestBody RegisterRequest request) {
        try {
            authenticationService.validateRegistrationRequest(
                request.getUsername(), 
                request.getPassword(), 
                request.getEmail()
            );

            authenticationService.handleRegistration(
                request.getUsername(),
                request.getPassword(),
                request.getEmail(),
                User.UserRole.SUPPORT
            );
            return ResponseEntity.ok("Support user created successfully");
        } catch (IllegalArgumentException e) {
            log.warn("Support registration validation failed: {}", e.getMessage());
            return ResponseEntity.status(400).body(e.getMessage());
        } catch (Exception e) {
            log.error("Support registration failed", e);
            return ResponseEntity.status(400).body("Support registration failed");
        }
    }

    public static class RegisterRequest {
        private String username;
        private String password;
        private String email;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}
