package com.vsign.backend.learning.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SubmitSignatureAttemptRequest(
        String userStoryId,

        @NotBlank
        String practiceItemId,

        String documentUploadId,

        @NotBlank
        String signatureVector,

        @Min(1)
        long durationMs,

        String aiStatus,
        String targetGloss,
        String predictedGloss,

        @DecimalMin("0.0")
        @DecimalMax("1.0")
        Double confidence,

        Boolean correct,

        @Min(0)
        Integer framesProcessed,

        @Min(0)
        Integer handsDetectedFrames,

        @DecimalMin("0.0")
        Double inferenceMs
) {
}
