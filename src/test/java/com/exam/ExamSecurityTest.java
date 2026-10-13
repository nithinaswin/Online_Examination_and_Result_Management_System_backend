package com.exam;

import com.exam.config.JwtTokenProvider;
import com.exam.dto.ExamRequest;
import com.exam.entity.Exam;
import com.exam.entity.ExamAttempt;
import com.exam.entity.Role;
import com.exam.entity.User;
import com.exam.repository.ExamAttemptRepository;
import com.exam.repository.ExamRepository;
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

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ExamSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ExamAttemptRepository attemptRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String student1Token;
    private String student2Token;
    private String adminToken;
    private User student1;
    private User student2;
    private User admin;
    private Exam exam;
    private ExamAttempt student1Attempt;

    @BeforeEach
    public void setup() {
        attemptRepository.deleteAll();
        examRepository.deleteAll();
        userRepository.deleteAll();

        student1 = userRepository.save(new User("student1@test.com", passwordEncoder.encode("pass123"), "Student One", Role.STUDENT));
        student2 = userRepository.save(new User("student2@test.com", passwordEncoder.encode("pass123"), "Student Two", Role.STUDENT));
        admin = userRepository.save(new User("admin@test.com", passwordEncoder.encode("pass123"), "Admin User", Role.ADMIN));

        student1Token = "Bearer " + jwtTokenProvider.generateToken(student1);
        student2Token = "Bearer " + jwtTokenProvider.generateToken(student2);
        adminToken = "Bearer " + jwtTokenProvider.generateToken(admin);

        exam = new Exam();
        exam.setTitle("CS101 Final Exam");
        exam.setDurationMinutes(30);
        exam.setPassMarks(5);
        exam.setActive(true);
        exam = examRepository.save(exam);

        student1Attempt = new ExamAttempt(student1, exam, LocalDateTime.now(), LocalDateTime.now().plusMinutes(30));
        student1Attempt.setTotalMarks(10);
        student1Attempt.setScore(8);
        student1Attempt = attemptRepository.save(student1Attempt);
    }

    @Test
    public void testStudentCannotAccessAdminEndpoints() throws Exception {
        ExamRequest request = new ExamRequest();
        request.setTitle("Hacked Exam");
        request.setDurationMinutes(10);
        request.setPassMarks(5);

        // Student trying to create exam -> 403 Forbidden
        mockMvc.perform(post("/api/exams")
                .header("Authorization", student1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Student trying to access admin dashboard stats -> 403 Forbidden
        mockMvc.perform(get("/api/admin/dashboard/stats")
                .header("Authorization", student1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testStudentCannotReadAnotherStudentsResult() throws Exception {
        // Student 2 trying to read Student 1's attempt result -> 403 Forbidden
        mockMvc.perform(get("/api/student/attempts/" + student1Attempt.getId() + "/result")
                .header("Authorization", student2Token))
                .andExpect(status().isForbidden());

        // Student 1 reading own attempt result -> 200 OK
        mockMvc.perform(get("/api/student/attempts/" + student1Attempt.getId() + "/result")
                .header("Authorization", student1Token))
                .andExpect(status().isOk());

        // Admin reading student 1's attempt result -> 200 OK
        mockMvc.perform(get("/api/admin/attempts/" + student1Attempt.getId() + "/result")
                .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }
}
