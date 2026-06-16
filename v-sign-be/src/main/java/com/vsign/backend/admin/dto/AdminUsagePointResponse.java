package com.vsign.backend.admin.dto;

public record AdminUsagePointResponse(
        String date,
        int activeSeconds,
        int lessonCompletions,
        int quizAttempts,
        int aiAttempts
) {
}
