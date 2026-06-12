package com.vsign.backend.learning.dto;

public record LessonSummaryResponse(
        String lessonId,
        String title,
        String description,
        String videoUrl,
        int durationSeconds,
        int orderIndex,
        boolean requiresPremium,
        boolean locked,
        String status
) {
}
