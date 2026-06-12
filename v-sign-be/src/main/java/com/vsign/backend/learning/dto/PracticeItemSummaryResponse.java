package com.vsign.backend.learning.dto;

public record PracticeItemSummaryResponse(
        String itemId,
        String lessonId,
        String label,
        String category,
        String level,
        String expectedGloss,
        String sourceVideoFile,
        String videoUrl
) {
}
