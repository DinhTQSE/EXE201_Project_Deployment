package com.vsign.backend.admin.dto;

import java.util.List;

public record AdminMetricsOverviewResponse(
        int totalUsers,
        int newUsers,
        int activeUsers,
        int activeUsersInRange,
        int premiumUsers,
        long totalRevenueVnd,
        int successfulPayments,
        int pendingReviews,
        int lessonCompletions,
        int quizAttempts,
        int aiAttempts,
        double aiSuccessRate,
        int averageActiveSeconds,
        List<AdminTopUserResponse> topActiveUsers
) {
}
