package com.vsign.backend.assessment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;

public record SubmitAttemptRequest(
        @Valid
        List<QuizAnswerRequest> answers,

        @Min(0)
        int durationSeconds
) {
}
