package com.vsign.backend.assessment.dto;

import jakarta.validation.constraints.NotBlank;

public record QuizAnswerRequest(
        @NotBlank
        String questionId,

        @NotBlank
        String selectedAnswerId
) {
}
