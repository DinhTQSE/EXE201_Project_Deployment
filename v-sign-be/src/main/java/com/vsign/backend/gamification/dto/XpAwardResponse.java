package com.vsign.backend.gamification.dto;

public record XpAwardResponse(
        String userId,
        String eventId,
        int totalXp,
        int xpAwarded,
        int currentStreak,
        int longestStreak,
        boolean duplicate
) {
}
