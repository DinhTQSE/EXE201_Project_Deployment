package com.vsign.backend.assessment.dto;

import java.util.List;

public record QuizReviewResponse(
        String attemptId,
        int score,
        boolean passed,
        List<QuizReviewQuestionResponse> questions
) {
}
