package com.exam.controller;

import com.exam.config.UserDetailsImpl;
import com.exam.dto.*;
import com.exam.service.AttemptService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

@RestController
public class AttemptController {

    private final AttemptService attemptService;

    public AttemptController(AttemptService attemptService) {
        this.attemptService = attemptService;
    }

    // --- STUDENT ENDPOINTS ---

    @PostMapping("/api/student/exams/{examId}/start")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<ExamStartResponse>> startExam(
            @PathVariable Long examId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ExamStartResponse response = attemptService.startOrResumeAttempt(examId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Exam attempt started", response));
    }

    @PostMapping("/api/student/attempts/{attemptId}/answers")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Void>> saveAnswer(
            @PathVariable Long attemptId,
            @Valid @RequestBody SaveAnswerRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        attemptService.saveAnswer(attemptId, userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Answer recorded", null));
    }

    @PostMapping("/api/student/attempts/{attemptId}/tab-switch")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> recordTabSwitch(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Map<String, Object> result = attemptService.recordTabSwitch(attemptId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Tab switch recorded", result));
    }

    @PostMapping("/api/student/attempts/{attemptId}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<ResultResponse>> submitExam(
            @PathVariable Long attemptId,
            @RequestBody(required = false) SubmitExamRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ResultResponse response = attemptService.submitAttempt(attemptId, userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Exam submitted and evaluated successfully", response));
    }

    @GetMapping("/api/student/attempts/{attemptId}/result")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<ResultResponse>> getStudentResult(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ResultResponse response = attemptService.getAttemptResult(attemptId, userDetails.getId(), false);
        return ResponseEntity.ok(ApiResponse.success("Result fetched", response));
    }

    @GetMapping("/api/student/attempts")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<PageResponse<ResultResponse>>> getStudentHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<ResultResponse> response = attemptService.getStudentAttemptHistory(userDetails.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Attempt history fetched", response));
    }

    // --- ADMIN ENDPOINTS ---

    @GetMapping("/api/admin/exams/{examId}/results")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<ResultResponse>>> getExamResultsAdmin(
            @PathVariable Long examId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<ResultResponse> response = attemptService.getExamAttemptsForAdmin(examId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Exam results fetched", response));
    }

    @GetMapping("/api/admin/attempts/{attemptId}/result")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ResultResponse>> getAdminResult(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ResultResponse response = attemptService.getAttemptResult(attemptId, userDetails.getId(), true);
        return ResponseEntity.ok(ApiResponse.success("Admin result fetched", response));
    }

    @GetMapping("/api/admin/exams/{examId}/results/export")
    @PreAuthorize("hasRole('ADMIN')")
    public void exportExamResultsCsv(
            @PathVariable Long examId,
            HttpServletResponse response) throws IOException {
        List<ResultResponse> results = attemptService.getLeaderboard(examId);

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"exam_" + examId + "_results.csv\"");

        PrintWriter writer = response.getWriter();
        writer.println("Rank,Student Name,Student Email,Score,Total Marks,Percentage,Passed,Status,Submitted At");

        int rank = 1;
        for (ResultResponse r : results) {
            writer.println(String.format("%d,\"%s\",\"%s\",%d,%d,%.2f,%b,%s,%s",
                    rank++,
                    r.getStudentName() != null ? r.getStudentName().replace("\"", "\"\"") : "",
                    r.getStudentEmail() != null ? r.getStudentEmail().replace("\"", "\"\"") : "",
                    r.getScore() != null ? r.getScore() : 0,
                    r.getTotalMarks() != null ? r.getTotalMarks() : 0,
                    r.getPercentage() != null ? r.getPercentage() : 0.0,
                    r.getPassed() != null && r.getPassed(),
                    r.getStatus(),
                    r.getSubmittedAt() != null ? r.getSubmittedAt().toString() : ""
            ));
        }
        writer.flush();
    }

    // --- LEADERBOARD ENDPOINT ---

    @GetMapping("/api/exams/{examId}/leaderboard")
    public ResponseEntity<ApiResponse<List<ResultResponse>>> getLeaderboard(@PathVariable Long examId) {
        List<ResultResponse> leaderboard = attemptService.getLeaderboard(examId);
        return ResponseEntity.ok(ApiResponse.success("Leaderboard fetched", leaderboard));
    }

    @GetMapping("/api/admin/exams/{examId}/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ExamAnalyticsResponse>> getExamAnalytics(@PathVariable Long examId) {
        ExamAnalyticsResponse analytics = attemptService.getExamAnalytics(examId);
        return ResponseEntity.ok(ApiResponse.success("Exam analytics fetched", analytics));
    }
}
