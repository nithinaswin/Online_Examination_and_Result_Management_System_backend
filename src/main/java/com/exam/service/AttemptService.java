package com.exam.service;

import com.exam.dto.*;
import com.exam.entity.*;
import com.exam.exception.BadRequestException;
import com.exam.exception.ForbiddenException;
import com.exam.exception.ResourceNotFoundException;
import com.exam.repository.AttemptAnswerRepository;
import com.exam.repository.ExamAttemptRepository;
import com.exam.repository.ExamRepository;
import com.exam.repository.QuestionRepository;
import com.exam.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AttemptService {

    private final ExamAttemptRepository attemptRepository;
    private final AttemptAnswerRepository answerRepository;
    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    @Value("${app.exam.grace-period-seconds:15}")
    private int gracePeriodSeconds;

    private static final int MAX_TAB_SWITCHES = 5;

    public AttemptService(ExamAttemptRepository attemptRepository,
                          AttemptAnswerRepository answerRepository,
                          ExamRepository examRepository,
                          QuestionRepository questionRepository,
                          UserRepository userRepository) {
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.examRepository = examRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ExamStartResponse startOrResumeAttempt(Long examId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found with id: " + examId));

        if (!exam.isActive()) {
            throw new BadRequestException("This exam is currently not active");
        }

        LocalDateTime now = LocalDateTime.now();
        if (exam.getWindowStart() != null && now.isBefore(exam.getWindowStart())) {
            throw new BadRequestException("This exam has not started yet. Window starts at " + exam.getWindowStart());
        }
        if (exam.getWindowEnd() != null && now.isAfter(exam.getWindowEnd())) {
            throw new BadRequestException("This exam window has closed. Closed at " + exam.getWindowEnd());
        }

        Optional<ExamAttempt> existingAttemptOpt = attemptRepository.findByUserIdAndExamId(userId, examId);

        ExamAttempt attempt;
        if (existingAttemptOpt.isPresent()) {
            attempt = existingAttemptOpt.get();
            if (attempt.getStatus() == AttemptStatus.COMPLETED || attempt.getStatus() == AttemptStatus.EXPIRED) {
                throw new BadRequestException("You have already completed your attempt for this exam.");
            }

            // Check if deadline has passed while away
            if (now.isAfter(attempt.getDeadline().plusSeconds(gracePeriodSeconds))) {
                autoFinalizeAttempt(attempt);
                throw new BadRequestException("Your exam time has expired.");
            }
        } else {
            // New attempt - Server strictly decides start time and deadline
            LocalDateTime startTime = LocalDateTime.now();
            LocalDateTime deadline = startTime.plusMinutes(exam.getDurationMinutes());

            attempt = new ExamAttempt(user, exam, startTime, deadline);
            attempt.setTotalMarks(exam.getTotalMarks());
            attempt = attemptRepository.save(attempt);
        }

        // Questions without correct options (strictly student view)
        List<Question> questions = questionRepository.findByExamIdOrderByIdAsc(examId);
        List<StudentQuestionResponse> studentQuestions = questions.stream()
                .map(q -> new StudentQuestionResponse(
                        q.getId(),
                        q.getQuestionText(),
                        q.getOptionA(),
                        q.getOptionB(),
                        q.getOptionC(),
                        q.getOptionD(),
                        q.getMarks()
                ))
                .toList();

        // Load any previously saved answers
        List<AttemptAnswer> savedAnswersList = answerRepository.findByAttemptIdOrderByIdAsc(attempt.getId());
        Map<Long, String> savedAnswersMap = savedAnswersList.stream()
                .filter(a -> a.getSelectedOption() != null)
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), AttemptAnswer::getSelectedOption, (a1, a2) -> a2));

        return new ExamStartResponse(
                attempt.getId(),
                exam.getId(),
                exam.getTitle(),
                exam.getDurationMinutes(),
                attempt.getStartTime(),
                attempt.getDeadline(),
                studentQuestions,
                savedAnswersMap,
                attempt.getTabSwitchCount()
        );
    }

    @Transactional
    public void saveAnswer(Long attemptId, Long userId, SaveAnswerRequest request) {
        ExamAttempt attempt = getVerifiedAttempt(attemptId, userId);

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new BadRequestException("Cannot save answers for an attempt that is " + attempt.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(attempt.getDeadline().plusSeconds(gracePeriodSeconds))) {
            autoFinalizeAttempt(attempt);
            throw new BadRequestException("Exam deadline has passed. Answers can no longer be recorded.");
        }

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + request.getQuestionId()));

        if (!question.getExam().getId().equals(attempt.getExam().getId())) {
            throw new BadRequestException("Question does not belong to this exam attempt");
        }

        Optional<AttemptAnswer> existingAnswerOpt =
                answerRepository.findByAttemptIdAndQuestionId(attemptId, request.getQuestionId());

        AttemptAnswer answer = existingAnswerOpt.orElseGet(() -> new AttemptAnswer(attempt, question, null));
        answer.setSelectedOption(request.getSelectedOption());
        answerRepository.save(answer);
    }

    @Transactional
    public Map<String, Object> recordTabSwitch(Long attemptId, Long userId) {
        ExamAttempt attempt = getVerifiedAttempt(attemptId, userId);

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new BadRequestException("Attempt is not in progress");
        }

        int count = attempt.getTabSwitchCount() + 1;
        attempt.setTabSwitchCount(count);
        attemptRepository.save(attempt);

        boolean autoSubmitted = false;
        if (count >= MAX_TAB_SWITCHES) {
            autoFinalizeAttempt(attempt);
            autoSubmitted = true;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("tabSwitchCount", count);
        result.put("maxAllowed", MAX_TAB_SWITCHES);
        result.put("autoSubmitted", autoSubmitted);
        return result;
    }

    @Transactional
    public ResultResponse submitAttempt(Long attemptId, Long userId, SubmitExamRequest request) {
        ExamAttempt attempt = getVerifiedAttempt(attemptId, userId);

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new BadRequestException("Attempt has already been submitted or finalized");
        }

        LocalDateTime now = LocalDateTime.now();
        boolean isExpired = now.isAfter(attempt.getDeadline().plusSeconds(gracePeriodSeconds));

        // Save any answers submitted in the final payload
        if (request != null && request.getAnswers() != null && !request.getAnswers().isEmpty()) {
            for (SaveAnswerRequest ansReq : request.getAnswers()) {
                Question question = questionRepository.findById(ansReq.getQuestionId()).orElse(null);
                if (question != null && question.getExam().getId().equals(attempt.getExam().getId())) {
                    AttemptAnswer answer = answerRepository
                            .findByAttemptIdAndQuestionId(attemptId, ansReq.getQuestionId())
                            .orElseGet(() -> new AttemptAnswer(attempt, question, null));
                    answer.setSelectedOption(ansReq.getSelectedOption());
                    answerRepository.save(answer);
                }
            }
        }

        return evaluateAndFinalizeAttempt(attempt, isExpired ? AttemptStatus.EXPIRED : AttemptStatus.COMPLETED);
    }

    @Transactional
    public ResultResponse autoFinalizeAttempt(ExamAttempt attempt) {
        return evaluateAndFinalizeAttempt(attempt, AttemptStatus.EXPIRED);
    }

    private ResultResponse evaluateAndFinalizeAttempt(ExamAttempt attempt, AttemptStatus finalStatus) {
        Exam exam = attempt.getExam();
        List<Question> questions = questionRepository.findByExamIdOrderByIdAsc(exam.getId());
        List<AttemptAnswer> answers = answerRepository.findByAttemptIdOrderByIdAsc(attempt.getId());

        Map<Long, AttemptAnswer> answerMap = answers.stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), a -> a, (a1, a2) -> a2));

        int totalScore = 0;
        int totalMarks = 0;
        List<QuestionReviewResponse> reviews = new ArrayList<>();

        for (Question q : questions) {
            int qMarks = q.getMarks();
            totalMarks += qMarks;

            AttemptAnswer ans = answerMap.get(q.getId());
            String selected = (ans != null) ? ans.getSelectedOption() : null;
            boolean isCorrect = selected != null && selected.equalsIgnoreCase(q.getCorrectOption());
            int awarded = isCorrect ? qMarks : 0;
            totalScore += awarded;

            if (ans != null) {
                ans.setIsCorrect(isCorrect);
                ans.setMarksAwarded(awarded);
                answerRepository.save(ans);
            } else {
                AttemptAnswer emptyAns = new AttemptAnswer(attempt, q, null);
                emptyAns.setIsCorrect(false);
                emptyAns.setMarksAwarded(0);
                answerRepository.save(emptyAns);
            }

            reviews.add(new QuestionReviewResponse(
                    q.getId(),
                    q.getQuestionText(),
                    q.getOptionA(),
                    q.getOptionB(),
                    q.getOptionC(),
                    q.getOptionD(),
                    selected,
                    q.getCorrectOption(),
                    isCorrect,
                    awarded,
                    qMarks
            ));
        }

        double percentage = (totalMarks > 0) ? ((double) totalScore / totalMarks) * 100.0 : 0.0;
        percentage = Math.round(percentage * 100.0) / 100.0;
        boolean passed = totalScore >= exam.getPassMarks();

        attempt.setScore(totalScore);
        attempt.setTotalMarks(totalMarks);
        attempt.setPercentage(percentage);
        attempt.setPassed(passed);
        attempt.setStatus(finalStatus);
        attempt.setSubmittedAt(LocalDateTime.now());

        ExamAttempt savedAttempt = attemptRepository.save(attempt);

        return buildResultResponse(savedAttempt, reviews);
    }

    @Transactional(readOnly = true)
    public ResultResponse getAttemptResult(Long attemptId, Long currentUserId, boolean isAdmin) {
        ExamAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found with id: " + attemptId));

        // Security check: Student can only view their own result. Prevent ID guessing!
        if (!isAdmin && !attempt.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("You are not authorized to view this result");
        }

        Exam exam = attempt.getExam();
        List<Question> questions = questionRepository.findByExamIdOrderByIdAsc(exam.getId());
        List<AttemptAnswer> answers = answerRepository.findByAttemptIdOrderByIdAsc(attempt.getId());

        Map<Long, AttemptAnswer> answerMap = answers.stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), a -> a, (a1, a2) -> a2));

        List<QuestionReviewResponse> reviews = new ArrayList<>();
        for (Question q : questions) {
            AttemptAnswer ans = answerMap.get(q.getId());
            String selected = (ans != null) ? ans.getSelectedOption() : null;
            boolean isCorrect = ans != null && Boolean.TRUE.equals(ans.getIsCorrect());
            int awarded = (ans != null && ans.getMarksAwarded() != null) ? ans.getMarksAwarded() : 0;

            reviews.add(new QuestionReviewResponse(
                    q.getId(),
                    q.getQuestionText(),
                    q.getOptionA(),
                    q.getOptionB(),
                    q.getOptionC(),
                    q.getOptionD(),
                    selected,
                    q.getCorrectOption(),
                    isCorrect,
                    awarded,
                    q.getMarks()
            ));
        }

        return buildResultResponse(attempt, reviews);
    }

    @Transactional(readOnly = true)
    public PageResponse<ResultResponse> getStudentAttemptHistory(Long studentId, Pageable pageable) {
        Page<ExamAttempt> attempts = attemptRepository.findByUserIdOrderByStartTimeDesc(studentId, pageable);
        Page<ResultResponse> mapped = attempts.map(a -> buildResultResponse(a, null));
        return new PageResponse<>(mapped);
    }

    @Transactional(readOnly = true)
    public PageResponse<ResultResponse> getExamAttemptsForAdmin(Long examId, Pageable pageable) {
        if (!examRepository.existsById(examId)) {
            throw new ResourceNotFoundException("Exam not found with id: " + examId);
        }
        Page<ExamAttempt> attempts = attemptRepository.findByExamIdOrderByScoreDesc(examId, pageable);
        Page<ResultResponse> mapped = attempts.map(a -> buildResultResponse(a, null));
        return new PageResponse<>(mapped);
    }

    @Transactional(readOnly = true)
    public List<ResultResponse> getLeaderboard(Long examId) {
        if (!examRepository.existsById(examId)) {
            throw new ResourceNotFoundException("Exam not found with id: " + examId);
        }
        List<ExamAttempt> leaderboard = attemptRepository.findLeaderboardByExamId(examId);
        return leaderboard.stream()
                .map(a -> buildResultResponse(a, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public com.exam.dto.ExamAnalyticsResponse getExamAnalytics(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found with id: " + examId));

        List<ExamAttempt> attempts = attemptRepository.findByExamIdOrderByScoreDesc(examId)
                .stream()
                .filter(a -> a.getStatus() == AttemptStatus.COMPLETED)
                .toList();

        long count = attempts.size();
        if (count == 0) {
            return new com.exam.dto.ExamAnalyticsResponse(exam.getId(), exam.getTitle(), 0, 0.0, 0, 0, 0.0);
        }

        double totalScore = attempts.stream().mapToInt(a -> a.getScore() != null ? a.getScore() : 0).sum();
        double avg = Math.round((totalScore / count) * 100.0) / 100.0;
        int max = attempts.stream().mapToInt(a -> a.getScore() != null ? a.getScore() : 0).max().orElse(0);
        int min = attempts.stream().mapToInt(a -> a.getScore() != null ? a.getScore() : 0).min().orElse(0);
        long passedCount = attempts.stream().filter(a -> Boolean.TRUE.equals(a.getPassed())).count();
        double passPct = Math.round(((double) passedCount / count * 100.0) * 100.0) / 100.0;

        return new com.exam.dto.ExamAnalyticsResponse(exam.getId(), exam.getTitle(), count, avg, max, min, passPct);
    }

    private ExamAttempt getVerifiedAttempt(Long attemptId, Long userId) {
        ExamAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found with id: " + attemptId));

        if (!attempt.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You are not authorized to access this exam attempt");
        }

        return attempt;
    }

    private ResultResponse buildResultResponse(ExamAttempt attempt, List<QuestionReviewResponse> reviews) {
        ResultResponse resp = new ResultResponse();
        resp.setAttemptId(attempt.getId());
        resp.setExamId(attempt.getExam().getId());
        resp.setExamTitle(attempt.getExam().getTitle());
        resp.setStudentId(attempt.getUser().getId());
        resp.setStudentName(attempt.getUser().getFullName());
        resp.setStudentEmail(attempt.getUser().getEmail());
        resp.setStartTime(attempt.getStartTime());
        resp.setDeadline(attempt.getDeadline());
        resp.setSubmittedAt(attempt.getSubmittedAt());
        resp.setScore(attempt.getScore());
        resp.setTotalMarks(attempt.getTotalMarks());
        resp.setPassMarks(attempt.getExam().getPassMarks());
        resp.setPercentage(attempt.getPercentage());
        resp.setPassed(attempt.getPassed());
        resp.setStatus(attempt.getStatus().name());
        resp.setTabSwitchCount(attempt.getTabSwitchCount());
        resp.setQuestionsReview(reviews);
        return resp;
    }
}
