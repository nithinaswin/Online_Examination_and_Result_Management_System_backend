package com.exam.service;

import com.exam.dto.ExamRequest;
import com.exam.dto.ExamResponse;
import com.exam.dto.PageResponse;
import com.exam.entity.Exam;
import com.exam.entity.ExamAttempt;
import com.exam.exception.BadRequestException;
import com.exam.exception.ResourceNotFoundException;
import com.exam.repository.ExamAttemptRepository;
import com.exam.repository.ExamRepository;
import com.exam.repository.QuestionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ExamService {

    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final ExamAttemptRepository examAttemptRepository;

    public ExamService(ExamRepository examRepository,
                       QuestionRepository questionRepository,
                       ExamAttemptRepository examAttemptRepository) {
        this.examRepository = examRepository;
        this.questionRepository = questionRepository;
        this.examAttemptRepository = examAttemptRepository;
    }

    @Transactional
    public ExamResponse createExam(ExamRequest request) {
        validateExamWindow(request);

        Exam exam = new Exam();
        exam.setTitle(request.getTitle().trim());
        exam.setDescription(request.getDescription());
        exam.setDurationMinutes(request.getDurationMinutes());
        exam.setWindowStart(request.getWindowStart());
        exam.setWindowEnd(request.getWindowEnd());
        exam.setPassMarks(request.getPassMarks());
        exam.setActive(request.getActive() != null ? request.getActive() : true);
        exam.setTotalMarks(0);

        Exam saved = examRepository.save(exam);
        return mapToResponse(saved, null);
    }

    @Transactional(readOnly = true)
    public ExamResponse getExamById(Long id) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found with id: " + id));
        return mapToResponse(exam, null);
    }

    @Transactional(readOnly = true)
    public ExamResponse getExamByIdForStudent(Long id, Long studentId) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found with id: " + id));
        return mapToResponse(exam, studentId);
    }

    @Transactional
    public ExamResponse updateExam(Long id, ExamRequest request) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found with id: " + id));

        validateExamWindow(request);

        exam.setTitle(request.getTitle().trim());
        exam.setDescription(request.getDescription());
        exam.setDurationMinutes(request.getDurationMinutes());
        exam.setWindowStart(request.getWindowStart());
        exam.setWindowEnd(request.getWindowEnd());
        exam.setPassMarks(request.getPassMarks());
        if (request.getActive() != null) {
            exam.setActive(request.getActive());
        }

        Exam updated = examRepository.save(exam);
        return mapToResponse(updated, null);
    }

    @Transactional
    public void deleteExam(Long id) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found with id: " + id));
        examRepository.delete(exam);
    }

    @Transactional(readOnly = true)
    public PageResponse<ExamResponse> getAllExamsAdmin(Pageable pageable) {
        Page<Exam> page = examRepository.findAll(pageable);
        Page<ExamResponse> mapped = page.map(e -> mapToResponse(e, null));
        return new PageResponse<>(mapped);
    }

    @Transactional(readOnly = true)
    public PageResponse<ExamResponse> getAvailableExamsForStudent(Long studentId, Pageable pageable) {
        Page<Exam> page = examRepository.findByActiveTrue(pageable);
        Page<ExamResponse> mapped = page.map(e -> mapToResponse(e, studentId));
        return new PageResponse<>(mapped);
    }

    private void validateExamWindow(ExamRequest request) {
        if (request.getWindowStart() != null && request.getWindowEnd() != null) {
            if (request.getWindowEnd().isBefore(request.getWindowStart())) {
                throw new BadRequestException("Exam window end time cannot be before window start time");
            }
        }
    }

    public ExamResponse mapToResponse(Exam exam, Long studentId) {
        ExamResponse resp = new ExamResponse();
        resp.setId(exam.getId());
        resp.setTitle(exam.getTitle());
        resp.setDescription(exam.getDescription());
        resp.setDurationMinutes(exam.getDurationMinutes());
        resp.setWindowStart(exam.getWindowStart());
        resp.setWindowEnd(exam.getWindowEnd());
        resp.setTotalMarks(exam.getTotalMarks());
        resp.setPassMarks(exam.getPassMarks());
        resp.setActive(exam.isActive());
        resp.setCreatedAt(exam.getCreatedAt());

        int count = questionRepository.findByExamIdOrderByIdAsc(exam.getId()).size();
        resp.setQuestionCount(count);

        if (studentId != null) {
            Optional<ExamAttempt> attempt = examAttemptRepository.findByUserIdAndExamId(studentId, exam.getId());
            if (attempt.isPresent()) {
                resp.setAttempted(true);
                resp.setAttemptId(attempt.get().getId());
            } else {
                resp.setAttempted(false);
                resp.setAttemptId(null);
            }
        }

        return resp;
    }
}
