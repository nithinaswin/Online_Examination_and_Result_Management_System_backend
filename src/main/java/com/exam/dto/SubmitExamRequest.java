package com.exam.dto;

import java.util.List;

public class SubmitExamRequest {

    private List<SaveAnswerRequest> answers;

    public SubmitExamRequest() {
    }

    public SubmitExamRequest(List<SaveAnswerRequest> answers) {
        this.answers = answers;
    }

    public List<SaveAnswerRequest> getAnswers() {
        return answers;
    }

    public void setAnswers(List<SaveAnswerRequest> answers) {
        this.answers = answers;
    }
}
