package com.vsign.backend.assessment.dto;

import jakarta.validation.constraints.NotBlank;

public record AnswerRequest(
        @NotBlank
        String questionId,

        String selectedOptionId,
        String selectedAnswerId
) {
    public String selectedAnswer() {
        return selectedAnswerId != null ? selectedAnswerId : selectedOptionId;
    }
}
