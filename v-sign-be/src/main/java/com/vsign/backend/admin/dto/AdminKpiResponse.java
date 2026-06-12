package com.vsign.backend.admin.dto;

public record AdminKpiResponse(
        int successfulTransactions,
        long totalRevenueVnd,
        int activeUsers,
        int premiumUsers,
        int pendingReviews
) {
}
