package com.vsign.backend.gamification.dto;

import java.util.List;

public record UserProgressSummaryResponse(
        String userId,
        int totalXp,
        int currentStreak,
        int longestStreak,
        List<BadgeResponse> badges
) {
}
