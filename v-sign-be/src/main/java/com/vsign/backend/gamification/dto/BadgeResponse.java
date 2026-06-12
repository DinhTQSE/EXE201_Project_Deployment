package com.vsign.backend.gamification.dto;

public record BadgeResponse(
        String badgeId,
        String name,
        String earnedAt
) {
}
