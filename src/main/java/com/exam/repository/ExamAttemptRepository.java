package com.exam.repository;

import com.exam.entity.AttemptStatus;
import com.exam.entity.ExamAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {

    Optional<ExamAttempt> findByUserIdAndExamId(Long userId, Long examId);

    boolean existsByUserIdAndExamId(Long userId, Long examId);

    Page<ExamAttempt> findByUserIdOrderByStartTimeDesc(Long userId, Pageable pageable);

    List<ExamAttempt> findByUserIdOrderByStartTimeDesc(Long userId);

    Page<ExamAttempt> findByExamIdOrderByScoreDesc(Long examId, Pageable pageable);

    List<ExamAttempt> findByExamIdOrderByScoreDesc(Long examId);

    long countByStatus(AttemptStatus status);

    @Query("SELECT AVG(a.score) FROM ExamAttempt a WHERE a.status = :status AND a.score IS NOT NULL")
    Double calculateAverageScore(@Param("status") AttemptStatus status);

    @Query("SELECT a FROM ExamAttempt a WHERE a.exam.id = :examId ORDER BY a.score DESC, a.submittedAt ASC")
    List<ExamAttempt> findLeaderboardByExamId(@Param("examId") Long examId);
}
