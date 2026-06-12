package com.vsign.backend.learning.dto;

import java.util.List;

public record PracticeItemDetailResponse(
        String itemId,
        String lessonId,
        String label,
        String category,
        String level,
        String expectedGloss,
        String sourceVideoFile,
        String videoUrl,
        List<String> rubric
) {
}
