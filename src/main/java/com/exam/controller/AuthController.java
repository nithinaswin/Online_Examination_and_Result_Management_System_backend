package com.exam.controller;

import com.exam.config.UserDetailsImpl;
import com.exam.dto.ApiResponse;
import com.exam.dto.AuthResponse;
import com.exam.dto.LoginRequest;
import com.exam.dto.SignupRequest;
import com.exam.dto.UserResponse;
import com.exam.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/student/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> studentSignup(
            @Valid @RequestBody SignupRequest request,
            HttpServletRequest servletRequest) {
        String clientIp = getClientIp(servletRequest);
        AuthResponse response = authService.signup(request, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created successfully", response));
    }

    // Keep /signup alias for compatibility
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletRequest servletRequest) {
        return studentSignup(request, servletRequest);
    }

    @PostMapping("/student/login")
    public ResponseEntity<ApiResponse<AuthResponse>> studentLogin(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {
        String clientIp = getClientIp(servletRequest);
        AuthResponse response = authService.studentLogin(request, clientIp);
        return ResponseEntity.ok(ApiResponse.success("Student login successful", response));
    }

    @PostMapping("/admin/login")
    public ResponseEntity<ApiResponse<AuthResponse>> adminLogin(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {
        String clientIp = getClientIp(servletRequest);
        AuthResponse response = authService.adminLogin(request, clientIp);
        return ResponseEntity.ok(ApiResponse.success("Admin login successful", response));
    }

    // Deprecated general login endpoint delegating to student login
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {
        String clientIp = getClientIp(servletRequest);
        AuthResponse response = authService.studentLogin(request, clientIp);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }
        UserResponse response = new UserResponse(
                userDetails.getUser().getId(),
                userDetails.getUser().getEmail(),
                userDetails.getUser().getFullName(),
                userDetails.getUser().getRole().name(),
                true,
                userDetails.getUser().getCreatedAt()
        );
        return ResponseEntity.ok(ApiResponse.success("Current user fetched", response));
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
