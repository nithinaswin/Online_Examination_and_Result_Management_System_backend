package com.exam.service;

import com.exam.dto.QuestionRequest;
import com.exam.dto.QuestionResponse;
import com.exam.entity.Exam;
import com.exam.entity.Question;
import com.exam.exception.BadRequestException;
import com.exam.exception.ResourceNotFoundException;
import com.exam.repository.ExamRepository;
import com.exam.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final ExamRepository examRepository;
    private final ActivityLogService activityLogService;

    public QuestionService(QuestionRepository questionRepository,
                           ExamRepository examRepository,
                           ActivityLogService activityLogService) {
        this.questionRepository = questionRepository;
        this.examRepository = examRepository;
        this.activityLogService = activityLogService;
    }

    @Transactional
    public QuestionResponse addQuestion(Long examId, QuestionRequest request) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found with id: " + examId));

        validateQuestionRequest(request);

        Question question = new Question();
        question.setExam(exam);
        question.setQuestionText(request.getQuestionText().trim());
        question.setOptionA(request.getOptionA().trim());
        question.setOptionB(request.getOptionB().trim());
        question.setOptionC(request.getOptionC().trim());
        question.setOptionD(request.getOptionD().trim());
        question.setCorrectOption(request.getCorrectOption().trim().toUpperCase());
        question.setMarks(request.getMarks() != null ? request.getMarks() : 1);

        Question saved = questionRepository.save(question);
        recalculateTotalMarks(exam);
        activityLogService.log("ADMIN", "QUESTION_ADD", "Added question to exam: " + exam.getTitle(), null);

        return mapToResponse(saved);
    }

    @Transactional
    public List<QuestionResponse> bulkAddQuestions(Long examId, List<QuestionRequest> requests) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found with id: " + examId));

        List<Question> questions = new ArrayList<>();
        for (QuestionRequest req : requests) {
            validateQuestionRequest(req);
            Question question = new Question();
            question.setExam(exam);
            question.setQuestionText(req.getQuestionText().trim());
            question.setOptionA(req.getOptionA().trim());
            question.setOptionB(req.getOptionB().trim());
            question.setOptionC(req.getOptionC().trim());
            question.setOptionD(req.getOptionD().trim());
            question.setCorrectOption(req.getCorrectOption().trim().toUpperCase());
            question.setMarks(req.getMarks() != null ? req.getMarks() : 1);
            questions.add(question);
        }

        List<Question> savedList = questionRepository.saveAll(questions);
        recalculateTotalMarks(exam);
        activityLogService.log("ADMIN", "QUESTION_BULK_ADD", "Bulk added " + savedList.size() + " questions to exam: " + exam.getTitle(), null);

        return savedList.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestionsForExamAdmin(Long examId) {
        if (!examRepository.existsById(examId)) {
            throw new ResourceNotFoundException("Exam not found with id: " + examId);
        }
        return questionRepository.findByExamIdOrderByIdAsc(examId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuestionResponse getQuestionById(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + id));
        return mapToResponse(question);
    }

    @Transactional
    public QuestionResponse updateQuestion(Long id, QuestionRequest request) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + id));

        validateQuestionRequest(request);

        question.setQuestionText(request.getQuestionText().trim());
        question.setOptionA(request.getOptionA().trim());
        question.setOptionB(request.getOptionB().trim());
        question.setOptionC(request.getOptionC().trim());
        question.setOptionD(request.getOptionD().trim());
        question.setCorrectOption(request.getCorrectOption().trim().toUpperCase());
        question.setMarks(request.getMarks() != null ? request.getMarks() : 1);

        Question updated = questionRepository.save(question);
        recalculateTotalMarks(question.getExam());
        activityLogService.log("ADMIN", "QUESTION_UPDATE", "Updated question id: " + id, null);

        return mapToResponse(updated);
    }

    @Transactional
    public void deleteQuestion(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + id));
        Exam exam = question.getExam();
        questionRepository.delete(question);
        recalculateTotalMarks(exam);
        activityLogService.log("ADMIN", "QUESTION_DELETE", "Deleted question id: " + id, null);
    }

    private void recalculateTotalMarks(Exam exam) {
        List<Question> questions = questionRepository.findByExamIdOrderByIdAsc(exam.getId());
        int sum = questions.stream().mapToInt(Question::getMarks).sum();
        exam.setTotalMarks(sum);
        examRepository.save(exam);
    }

    private void validateQuestionRequest(QuestionRequest request) {
        String opt = request.getCorrectOption() != null ? request.getCorrectOption().trim().toUpperCase() : "";
        if (!opt.matches("^[A-D]$")) {
            throw new BadRequestException("Correct option must be one of: A, B, C, D");
        }
    }

    private QuestionResponse mapToResponse(Question q) {
        return new QuestionResponse(
                q.getId(),
                q.getExam().getId(),
                q.getQuestionText(),
                q.getOptionA(),
                q.getOptionB(),
                q.getOptionC(),
                q.getOptionD(),
                q.getCorrectOption(),
                q.getMarks()
        );
    }
}
