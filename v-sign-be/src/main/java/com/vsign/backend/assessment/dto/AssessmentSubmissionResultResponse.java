package com.vsign.backend.assessment.dto;

public record AssessmentSubmissionResultResponse(
        String assessmentId,
        String userId,
        int score,
        boolean passed,
        int correctAnswers,
        int totalQuestions,
        int awardedXp
) {
}
