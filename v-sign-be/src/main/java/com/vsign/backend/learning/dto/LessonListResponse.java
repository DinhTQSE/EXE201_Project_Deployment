package com.vsign.backend.learning.dto;

import java.util.List;

public record LessonListResponse(
        String chapterId,
        List<LessonSummaryResponse> lessons
) {
}
