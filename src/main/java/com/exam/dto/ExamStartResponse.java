package com.exam.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ExamStartResponse {

    private Long attemptId;
    private Long examId;
    private String examTitle;
    private Integer durationMinutes;
    private LocalDateTime startTime;
    private LocalDateTime deadline;
    private List<StudentQuestionResponse> questions;
    private Map<Long, String> savedAnswers;
    private Integer tabSwitchCount;

    public ExamStartResponse() {
    }

    public ExamStartResponse(Long attemptId, Long examId, String examTitle, Integer durationMinutes,
                             LocalDateTime startTime, LocalDateTime deadline,
                             List<StudentQuestionResponse> questions,
                             Map<Long, String> savedAnswers, Integer tabSwitchCount) {
        this.attemptId = attemptId;
        this.examId = examId;
        this.examTitle = examTitle;
        this.durationMinutes = durationMinutes;
        this.startTime = startTime;
        this.deadline = deadline;
        this.questions = questions;
        this.savedAnswers = savedAnswers;
        this.tabSwitchCount = tabSwitchCount;
    }

    public Long getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public String getExamTitle() {
        return examTitle;
    }

    public void setExamTitle(String examTitle) {
        this.examTitle = examTitle;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public List<StudentQuestionResponse> getQuestions() {
        return questions;
    }

    public void setQuestions(List<StudentQuestionResponse> questions) {
        this.questions = questions;
    }

    public Map<Long, String> getSavedAnswers() {
        return savedAnswers;
    }

    public void setSavedAnswers(Map<Long, String> savedAnswers) {
        this.savedAnswers = savedAnswers;
    }

    public Integer getTabSwitchCount() {
        return tabSwitchCount;
    }

    public void setTabSwitchCount(Integer tabSwitchCount) {
        this.tabSwitchCount = tabSwitchCount;
    }
}
