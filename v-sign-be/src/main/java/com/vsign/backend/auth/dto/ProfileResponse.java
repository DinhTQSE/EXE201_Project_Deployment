package com.vsign.backend.auth.dto;

import java.util.List;

public record ProfileResponse(
        String id,
        String email,
        String fullName,
        String displayName,
        String avatarUrl,
        String bio,
        String role,
        String accountType,
        int totalXp,
        int currentStreak,
        int longestStreak,
        SubscriptionSummaryResponse subscription,
        List<BadgeSummaryResponse> badges
) {
    public record SubscriptionSummaryResponse(
            String planType,
            String status,
            String startDate,
            String endDate
    ) {
    }

    public record BadgeSummaryResponse(
            String badgeId,
            String name,
            String iconUrl,
            String earnedAt
    ) {
    }
}
