package com.exam.service;

import com.exam.dto.DashboardStatsResponse;
import com.exam.entity.AttemptStatus;
import com.exam.entity.Role;
import com.exam.repository.ExamAttemptRepository;
import com.exam.repository.ExamRepository;
import com.exam.repository.QuestionRepository;
import com.exam.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final ExamAttemptRepository attemptRepository;

    public DashboardService(ExamRepository examRepository,
                            QuestionRepository questionRepository,
                            UserRepository userRepository,
                            ExamAttemptRepository attemptRepository) {
        this.examRepository = examRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.attemptRepository = attemptRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        long totalExams = examRepository.count();
        long totalQuestions = questionRepository.count();
        long totalStudents = userRepository.countByRole(Role.STUDENT);
        long totalAttempts = attemptRepository.count();

        Double avg = attemptRepository.calculateAverageScore(AttemptStatus.COMPLETED);
        double averageScore = (avg != null) ? Math.round(avg * 100.0) / 100.0 : 0.0;

        return new DashboardStatsResponse(
                totalExams,
                totalQuestions,
                totalStudents,
                totalAttempts,
                averageScore
        );
    }
}
