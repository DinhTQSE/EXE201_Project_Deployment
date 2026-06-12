package com.vsign.backend.assessment.dto;

import java.util.List;

public record QuizResponse(
        String lessonId,
        String quizId,
        String attemptId,
        List<QuestionResponse> questions
) {
}
