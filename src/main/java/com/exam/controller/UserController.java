package com.exam.controller;

import com.exam.config.UserDetailsImpl;
import com.exam.dto.ApiResponse;
import com.exam.dto.PageResponse;
import com.exam.dto.PasswordChangeRequest;
import com.exam.dto.UserCreateRequest;
import com.exam.dto.UserResponse;
import com.exam.dto.UserUpdateRequest;
import com.exam.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // --- ADMIN USER MANAGEMENT ENDPOINTS ---

    @GetMapping("/api/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String role) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        PageResponse<UserResponse> response;
        if ("STUDENT".equalsIgnoreCase(role)) {
            response = userService.getAllStudents(pageable);
        } else {
            response = userService.getAllUsers(pageable);
        }
        return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", response));
    }

    @GetMapping("/api/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User fetched successfully", response));
    }

    @PostMapping("/api/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created successfully", response));
    }

    @PutMapping("/api/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", response));
    }

    @PutMapping("/api/admin/users/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody PasswordChangeRequest request) {
        UserResponse response = userService.resetPassword(id, request);
        return ResponseEntity.ok(ApiResponse.success("User password reset successfully", response));
    }

    @DeleteMapping("/api/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }

    // --- STUDENT SELF-MANAGEMENT ENDPOINTS ---

    @PutMapping("/api/student/profile")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody UserUpdateRequest request) {
        UserResponse response = userService.updateStudentProfile(
                userDetails.getId(),
                request.getFullName(),
                request.getPassword()
        );
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }
}
