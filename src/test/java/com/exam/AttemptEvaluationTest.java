package com.exam;

import com.exam.config.JwtTokenProvider;
import com.exam.dto.SaveAnswerRequest;
import com.exam.dto.SubmitExamRequest;
import com.exam.entity.*;
import com.exam.repository.*;
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
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AttemptEvaluationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ExamAttemptRepository attemptRepository;

    @Autowired
    private AttemptAnswerRepository answerRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User student;
    private String studentToken;
    private Exam exam;
    private Question q1;
    private Question q2;

    @BeforeEach
    public void setup() {
        answerRepository.deleteAll();
        attemptRepository.deleteAll();
        questionRepository.deleteAll();
        examRepository.deleteAll();
        userRepository.deleteAll();

        student = userRepository.save(new User("student@exam.com", passwordEncoder.encode("secret"), "Alice Student", Role.STUDENT));
        studentToken = "Bearer " + jwtTokenProvider.generateToken(student);

        exam = new Exam();
        exam.setTitle("Data Structures Quiz");
        exam.setDescription("Basic DS concepts");
        exam.setDurationMinutes(20);
        exam.setPassMarks(5);
        exam.setActive(true);
        exam.setTotalMarks(10);
        exam = examRepository.save(exam);

        q1 = new Question();
        q1.setExam(exam);
        q1.setQuestionText("What is the time complexity of binary search?");
        q1.setOptionA("O(n)");
        q1.setOptionB("O(log n)");
        q1.setOptionC("O(n^2)");
        q1.setOptionD("O(1)");
        q1.setCorrectOption("B");
        q1.setMarks(5);
        q1 = questionRepository.save(q1);

        q2 = new Question();
        q2.setExam(exam);
        q2.setQuestionText("Which data structure uses FIFO?");
        q2.setOptionA("Stack");
        q2.setOptionB("Queue");
        q2.setOptionC("Tree");
        q2.setOptionD("Graph");
        q2.setCorrectOption("B");
        q2.setMarks(5);
        q2 = questionRepository.save(q2);
    }

    @Test
    public void testStartExamNeverExposesCorrectOption() throws Exception {
        mockMvc.perform(post("/api/student/exams/" + exam.getId() + "/start")
                .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questions", hasSize(2)))
                .andExpect(jsonPath("$.data.questions[0].correctOption").doesNotExist())
                .andExpect(jsonPath("$.data.questions[1].correctOption").doesNotExist());
    }

    @Test
    public void testScoringCorrectnessOnSubmission() throws Exception {
        // Start exam
        String startResponse = mockMvc.perform(post("/api/student/exams/" + exam.getId() + "/start")
                .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long attemptId = objectMapper.readTree(startResponse).path("data").path("attemptId").asLong();

        // Submit: answer Q1 correctly (B -> 5 marks) and Q2 incorrectly (A -> 0 marks)
        SubmitExamRequest submitReq = new SubmitExamRequest(List.of(
                new SaveAnswerRequest(q1.getId(), "B"),
                new SaveAnswerRequest(q2.getId(), "A")
        ));

        mockMvc.perform(post("/api/student/attempts/" + attemptId + "/submit")
                .header("Authorization", studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score", is(5)))
                .andExpect(jsonPath("$.data.totalMarks", is(10)))
                .andExpect(jsonPath("$.data.percentage", is(50.0)))
                .andExpect(jsonPath("$.data.passed", is(true)))
                .andExpect(jsonPath("$.data.status", is("COMPLETED")));
    }

    @Test
    public void testDuplicateAttemptRejection() throws Exception {
        // First start
        mockMvc.perform(post("/api/student/exams/" + exam.getId() + "/start")
                .header("Authorization", studentToken))
                .andExpect(status().isOk());

        // Get attempt and submit it
        ExamAttempt attempt = attemptRepository.findByUserIdAndExamId(student.getId(), exam.getId()).orElseThrow();
        attempt.setStatus(AttemptStatus.COMPLETED);
        attemptRepository.save(attempt);

        // Try to start again -> rejected with 400 Bad Request
        mockMvc.perform(post("/api/student/exams/" + exam.getId() + "/start")
                .header("Authorization", studentToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already completed")));
    }

    @Test
    public void testLateSubmissionRejection() throws Exception {
        // Create an expired attempt (deadline in past)
        ExamAttempt expiredAttempt = new ExamAttempt(
                student,
                exam,
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusMinutes(30)
        );
        expiredAttempt = attemptRepository.save(expiredAttempt);

        // Try to save answer after deadline -> rejected
        SaveAnswerRequest answerReq = new SaveAnswerRequest(q1.getId(), "B");
        mockMvc.perform(post("/api/student/attempts/" + expiredAttempt.getId() + "/answers")
                .header("Authorization", studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(answerReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("deadline has passed")));
    }
}
