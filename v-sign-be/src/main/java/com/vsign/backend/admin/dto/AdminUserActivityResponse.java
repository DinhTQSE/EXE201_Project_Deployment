package com.vsign.backend.admin.dto;

public record AdminUserActivityResponse(
        int activeSeconds,
        int completedLessons,
        int quizAttempts,
        int aiAttempts,
        int aiPassedAttempts,
        String lastSeenAt
) {
}
