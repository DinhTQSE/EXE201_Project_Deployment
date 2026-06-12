package com.vsign.backend.learning.dto;

public record ProgressResponse(
        String lessonId,
        int completionPct,
        int lastPositionSeconds,
        String phase,
        Integer currentQuestionIndex,
        String status
) {
}
