package com.exam.service;

import com.exam.dto.PageResponse;
import com.exam.dto.PasswordChangeRequest;
import com.exam.dto.UserCreateRequest;
import com.exam.dto.UserResponse;
import com.exam.dto.UserUpdateRequest;
import com.exam.entity.Role;
import com.exam.entity.User;
import com.exam.exception.BadRequestException;
import com.exam.exception.ResourceNotFoundException;
import com.exam.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ActivityLogService activityLogService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       ActivityLogService activityLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.activityLogService = activityLogService;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(Pageable pageable) {
        Page<User> page = userRepository.findAll(pageable);
        return new PageResponse<>(page.map(this::mapToResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllStudents(Pageable pageable) {
        Page<User> page = userRepository.findByRole(Role.STUDENT, pageable);
        return new PageResponse<>(page.map(this::mapToResponse));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapToResponse(user);
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new BadRequestException("Email already in use");
        }

        User user = new User(
                request.getEmail().trim().toLowerCase(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName().trim(),
                request.getRole() != null ? request.getRole() : Role.STUDENT
        );

        User saved = userRepository.save(user);
        activityLogService.log("ADMIN", "USER_CREATE", "Created user: " + saved.getEmail(), null);
        return mapToResponse(saved);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        String newEmail = request.getEmail().trim().toLowerCase();
        if (!user.getEmail().equalsIgnoreCase(newEmail) && userRepository.existsByEmail(newEmail)) {
            throw new BadRequestException("Email already in use by another account");
        }

        user.setFullName(request.getFullName().trim());
        user.setEmail(newEmail);
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User updated = userRepository.save(user);
        activityLogService.log("ADMIN", "USER_UPDATE", "Updated user profile: " + updated.getEmail(), null);
        return mapToResponse(updated);
    }

    @Transactional
    public UserResponse resetPassword(Long id, PasswordChangeRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        User updated = userRepository.save(user);
        activityLogService.log("ADMIN", "PASSWORD_RESET", "Password reset for user: " + updated.getEmail(), null);
        return mapToResponse(updated);
    }

    @Transactional
    public UserResponse updateStudentProfile(Long userId, String fullName, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        if (StringUtils.hasText(fullName)) {
            user.setFullName(fullName.trim());
        }
        if (StringUtils.hasText(newPassword)) {
            if (newPassword.length() < 6) {
                throw new BadRequestException("Password must be at least 6 characters");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }
        User updated = userRepository.save(user);
        activityLogService.log(updated.getEmail(), "PROFILE_UPDATE", "Student updated profile", null);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        userRepository.delete(user);
        activityLogService.log("ADMIN", "USER_DELETE", "Deleted user: " + user.getEmail(), null);
    }

    private UserResponse mapToResponse(User u) {
        return new UserResponse(
                u.getId(),
                u.getEmail(),
                u.getFullName(),
                u.getRole().name(),
                true,
                u.getCreatedAt()
        );
    }
}
