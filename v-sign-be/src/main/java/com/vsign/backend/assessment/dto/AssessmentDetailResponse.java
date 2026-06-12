package com.vsign.backend.assessment.dto;

import java.util.List;

public record AssessmentDetailResponse(
        String id,
        String title,
        int passingScore,
        List<QuestionResponse> questions
) {
}
