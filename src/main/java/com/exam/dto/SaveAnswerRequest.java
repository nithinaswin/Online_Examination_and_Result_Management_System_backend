package com.exam.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class SaveAnswerRequest {

    @NotNull(message = "Question ID is required")
    private Long questionId;

    @Pattern(regexp = "^[A-D]$|^$", message = "Selected option must be A, B, C, D or empty")
    private String selectedOption;

    public SaveAnswerRequest() {
    }

    public SaveAnswerRequest(Long questionId, String selectedOption) {
        this.questionId = questionId;
        this.selectedOption = selectedOption;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getSelectedOption() {
        return selectedOption;
    }

    public void setSelectedOption(String selectedOption) {
        this.selectedOption = selectedOption;
    }
}
