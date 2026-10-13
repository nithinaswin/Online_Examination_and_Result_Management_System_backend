package com.exam;

import com.exam.dto.LoginRequest;
import com.exam.dto.SignupRequest;
import com.exam.entity.Role;
import com.exam.entity.User;
import com.exam.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    public void setup() {
        if (!userRepository.existsByEmail("admin_auth_test@college.edu")) {
            User admin = new User(
                    "admin_auth_test@college.edu",
                    passwordEncoder.encode("AdminPass123!"),
                    "Test Admin",
                    Role.ADMIN
            );
            userRepository.save(admin);
        }
    }

    @Test
    public void testStudentSignupAndLoginSuccess() throws Exception {
        SignupRequest signup = new SignupRequest("Jane Doe", "jane_unique@example.com", "Password123!");

        mockMvc.perform(post("/api/auth/student/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signup)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.role", is("STUDENT")))
                .andExpect(jsonPath("$.data.token", notNullValue()));

        // Student Login with student endpoint
        LoginRequest login = new LoginRequest("jane_unique@example.com", "Password123!");
        mockMvc.perform(post("/api/auth/student/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.role", is("STUDENT")))
                .andExpect(jsonPath("$.data.token", notNullValue()));
    }

    @Test
    public void testStudentCannotLoginViaAdminEndpoint() throws Exception {
        // Create student
        SignupRequest signup = new SignupRequest("Student Person", "student_strict@example.com", "Password123!");
        mockMvc.perform(post("/api/auth/student/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signup)))
                .andExpect(status().isCreated());

        // Student tries to login via /api/auth/admin/login -> must fail with 401
        LoginRequest login = new LoginRequest("student_strict@example.com", "Password123!");
        mockMvc.perform(post("/api/auth/admin/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Invalid credentials")));
    }

    @Test
    public void testAdminCannotLoginViaStudentEndpoint() throws Exception {
        // Admin tries to login via /api/auth/student/login -> must fail with 401
        LoginRequest login = new LoginRequest("admin_auth_test@college.edu", "AdminPass123!");
        mockMvc.perform(post("/api/auth/student/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Invalid credentials")));

        // Admin logs in via /api/auth/admin/login -> succeeds
        mockMvc.perform(post("/api/auth/admin/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.role", is("ADMIN")));
    }

    @Test
    public void testLoginWithInvalidCredentialsReturns401() throws Exception {
        LoginRequest login = new LoginRequest("nonexistent@example.com", "WrongPassword!");
        mockMvc.perform(post("/api/auth/student/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }
}
