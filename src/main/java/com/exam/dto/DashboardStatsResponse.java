package com.exam.dto;

public class DashboardStatsResponse {

    private long totalExams;
    private long totalQuestions;
    private long totalStudents;
    private long totalAttempts;
    private double averageScore;

    public DashboardStatsResponse() {
    }

    public DashboardStatsResponse(long totalExams, long totalQuestions, long totalStudents, long totalAttempts, double averageScore) {
        this.totalExams = totalExams;
        this.totalQuestions = totalQuestions;
        this.totalStudents = totalStudents;
        this.totalAttempts = totalAttempts;
        this.averageScore = averageScore;
    }

    public long getTotalExams() {
        return totalExams;
    }

    public void setTotalExams(long totalExams) {
        this.totalExams = totalExams;
    }

    public long getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(long totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
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
}
