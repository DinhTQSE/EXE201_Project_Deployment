package com.vsign.backend.monetization.dto;

public record SubscriptionPlanResponse(
        String planId,
        String planType,
        String name,
        int amount,
        int price,
        String currency,
        int durationDays,
        boolean active
) {
}
