package com.vsign.backend.assessment.dto;

public record AssessmentSummaryResponse(
        String id,
        String title,
        int questionCount,
        int passingScore
) {
}
