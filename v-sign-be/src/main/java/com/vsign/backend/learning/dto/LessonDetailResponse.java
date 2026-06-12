package com.vsign.backend.learning.dto;

public record LessonDetailResponse(
        String lessonId,
        String title,
        String videoUrl,
        boolean requiresPremium,
        LessonProgressCheckpointResponse progress
) {
}
