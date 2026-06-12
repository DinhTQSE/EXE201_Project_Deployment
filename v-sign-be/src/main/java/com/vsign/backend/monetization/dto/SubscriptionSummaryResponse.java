package com.vsign.backend.monetization.dto;

public record SubscriptionSummaryResponse(
        String planType,
        String status,
        String startedAt,
        String expiresAt,
        int remainingDays
) {
}
