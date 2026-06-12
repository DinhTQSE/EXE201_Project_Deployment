package com.vsign.backend.learning.dto;

import java.util.List;

public record SignatureAttemptResponse(
        String attemptId,
        String practiceItemId,
        String status,
        int score,
        String targetGloss,
        String predictedGloss,
        Double confidence,
        Boolean correct,
        List<String> feedbackCodes
) {
}
