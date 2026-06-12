package com.vsign.backend.learning.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateProgressRequest(
        @Min(0)
        @Max(100)
        int completionPct,

        @Min(0)
        int lastPositionSeconds,

        String phase,
        Integer currentQuestionIndex,
        ProgressStatus status
) {
}
