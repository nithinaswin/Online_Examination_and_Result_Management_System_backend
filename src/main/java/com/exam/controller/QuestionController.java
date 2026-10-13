package com.exam.controller;

import com.exam.dto.ApiResponse;
import com.exam.dto.QuestionRequest;
import com.exam.dto.QuestionResponse;
import com.exam.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @PostMapping("/exams/{examId}/questions")
    public ResponseEntity<ApiResponse<QuestionResponse>> addQuestion(
            @PathVariable Long examId,
            @Valid @RequestBody QuestionRequest request) {
        QuestionResponse response = questionService.addQuestion(examId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Question created successfully", response));
    }

    @PostMapping("/exams/{examId}/questions/bulk")
    public ResponseEntity<ApiResponse<List<QuestionResponse>>> bulkAddQuestions(
            @PathVariable Long examId,
            @Valid @RequestBody List<QuestionRequest> requests) {
        List<QuestionResponse> response = questionService.bulkAddQuestions(examId, requests);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Questions added successfully", response));
    }

    @GetMapping("/exams/{examId}/questions")
    public ResponseEntity<ApiResponse<List<QuestionResponse>>> getQuestionsForExam(@PathVariable Long examId) {
        List<QuestionResponse> questions = questionService.getQuestionsForExamAdmin(examId);
        return ResponseEntity.ok(ApiResponse.success("Questions fetched successfully", questions));
    }

    @GetMapping("/questions/{id}")
    public ResponseEntity<ApiResponse<QuestionResponse>> getQuestionById(@PathVariable Long id) {
        QuestionResponse response = questionService.getQuestionById(id);
        return ResponseEntity.ok(ApiResponse.success("Question fetched successfully", response));
    }

    @PutMapping("/questions/{id}")
    public ResponseEntity<ApiResponse<QuestionResponse>> updateQuestion(
            @PathVariable Long id,
            @Valid @RequestBody QuestionRequest request) {
        QuestionResponse response = questionService.updateQuestion(id, request);
        return ResponseEntity.ok(ApiResponse.success("Question updated successfully", response));
    }

    @DeleteMapping("/questions/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return ResponseEntity.ok(ApiResponse.success("Question deleted successfully", null));
    }
}
