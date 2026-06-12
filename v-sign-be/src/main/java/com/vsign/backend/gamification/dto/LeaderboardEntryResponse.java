package com.vsign.backend.gamification.dto;

public record LeaderboardEntryResponse(
        int rank,
        String userId,
        String fullName,
        String avatarUrl,
        int xp
) {
}
