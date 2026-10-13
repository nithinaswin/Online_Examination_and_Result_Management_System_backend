package com.exam.service;

import com.exam.config.JwtTokenProvider;
import com.exam.dto.AuthResponse;
import com.exam.dto.LoginRequest;
import com.exam.dto.SignupRequest;
import com.exam.entity.Role;
import com.exam.entity.User;
import com.exam.exception.BadRequestException;
import com.exam.exception.UnauthorizedException;
import com.exam.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final LoginRateLimiter rateLimiter;
    private final ActivityLogService activityLogService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       LoginRateLimiter rateLimiter,
                       ActivityLogService activityLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.rateLimiter = rateLimiter;
        this.activityLogService = activityLogService;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request, String ipAddress) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("An account with this email already exists");
        }

        // College Rule: Public student signup always creates STUDENT role.
        User user = new User(
                email,
                passwordEncoder.encode(request.getPassword()),
                request.getFullName().trim(),
                Role.STUDENT
        );

        user = userRepository.save(user);
        activityLogService.log(user.getEmail(), "SIGNUP", "Student account registered", ipAddress);

        String token = tokenProvider.generateToken(user);

        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse studentLogin(LoginRequest request, String ipAddress) {
        return authenticateWithRole(request, Role.STUDENT, ipAddress);
    }

    @Transactional(readOnly = true)
    public AuthResponse adminLogin(LoginRequest request, String ipAddress) {
        return authenticateWithRole(request, Role.ADMIN, ipAddress);
    }

    private AuthResponse authenticateWithRole(LoginRequest request, Role expectedRole, String ipAddress) {
        String email = request.getEmail().trim().toLowerCase();
        String rateLimitKey = email + ":" + (ipAddress != null ? ipAddress : "");

        if (rateLimiter.isBlocked(rateLimitKey)) {
            throw new BadRequestException("Account temporarily locked due to multiple failed attempts. Please try again in 5 minutes.");
        }

        User user = userRepository.findByEmail(email).orElse(null);

        // Generic error message to prevent enumeration & reject non-matching roles
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            rateLimiter.recordFailedAttempt(rateLimitKey);
            throw new UnauthorizedException("Invalid credentials");
        }

        // Enforce role separation strictly
        if (user.getRole() != expectedRole) {
            rateLimiter.recordFailedAttempt(rateLimitKey);
            throw new UnauthorizedException("Invalid credentials");
        }

        rateLimiter.recordSuccessfulAttempt(rateLimitKey);
        activityLogService.log(user.getEmail(), "LOGIN", "User logged in as " + user.getRole(), ipAddress);

        String token = tokenProvider.generateToken(user);

        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name()
        );
    }
}
