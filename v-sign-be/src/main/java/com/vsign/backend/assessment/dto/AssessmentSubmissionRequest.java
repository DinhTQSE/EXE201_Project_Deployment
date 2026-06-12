package com.vsign.backend.assessment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record AssessmentSubmissionRequest(
        @NotBlank
        String userId,

        @Valid
        List<AnswerRequest> answers
) {
}
