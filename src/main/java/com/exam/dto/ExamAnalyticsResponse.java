package com.exam.dto;

public class ExamAnalyticsResponse {
    private Long examId;
    private String examTitle;
    private long totalAttempts;
    private double averageScore;
    private int highestScore;
    private int lowestScore;
    private double passPercentage;

    public ExamAnalyticsResponse() {}

    public ExamAnalyticsResponse(Long examId, String examTitle, long totalAttempts, double averageScore, int highestScore, int lowestScore, double passPercentage) {
        this.examId = examId;
        this.examTitle = examTitle;
        this.totalAttempts = totalAttempts;
        this.averageScore = averageScore;
        this.highestScore = highestScore;
        this.lowestScore = lowestScore;
        this.passPercentage = passPercentage;
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

    public long getTotalAttempts() {
        return totalAttempts;
    }

    public void setTotalAttempts(long totalAttempts) {
        this.totalAttempts = totalAttempts;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(double averageScore) {
        this.averageScore = averageScore;
    }

    public int getHighestScore() {
        return highestScore;
    }

    public void setHighestScore(int highestScore) {
        this.highestScore = highestScore;
    }

    public int getLowestScore() {
        return lowestScore;
    }

    public void setLowestScore(int lowestScore) {
        this.lowestScore = lowestScore;
    }

    public double getPassPercentage() {
        return passPercentage;
    }

    public void setPassPercentage(double passPercentage) {
        this.passPercentage = passPercentage;
    }
}
