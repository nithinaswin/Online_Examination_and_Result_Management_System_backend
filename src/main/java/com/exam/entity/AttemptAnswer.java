package com.exam.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "attempt_answers",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_attempt_question", columnNames = {"attempt_id", "question_id"})
    },
    indexes = {
        @Index(name = "idx_ans_attempt_id", columnList = "attempt_id"),
        @Index(name = "idx_ans_question_id", columnList = "question_id")
    }
)
public class AttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private ExamAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "selected_option", length = 10)
    private String selectedOption;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "marks_awarded")
    private Integer marksAwarded = 0;

    public AttemptAnswer() {
    }

    public AttemptAnswer(ExamAttempt attempt, Question question, String selectedOption) {
        this.attempt = attempt;
        this.question = question;
        this.selectedOption = selectedOption;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ExamAttempt getAttempt() {
        return attempt;
    }

    public void setAttempt(ExamAttempt attempt) {
        this.attempt = attempt;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public String getSelectedOption() {
        return selectedOption;
    }

    public void setSelectedOption(String selectedOption) {
        this.selectedOption = selectedOption;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }

    public Integer getMarksAwarded() {
        return marksAwarded;
    }

    public void setMarksAwarded(Integer marksAwarded) {
        this.marksAwarded = marksAwarded;
    }
}
