package com.vsign.backend.learning.dto;

public record ChapterSummaryResponse(
        String chapterId,
        String title,
        String description,
        int lessonCount,
        int orderIndex,
        boolean requiresPremium,
        boolean locked,
        int completionPercent
) {
}
