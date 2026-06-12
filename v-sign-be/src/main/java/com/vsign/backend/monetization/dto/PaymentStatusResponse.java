package com.vsign.backend.monetization.dto;

public record PaymentStatusResponse(
        String transactionId,
        String providerTransactionId,
        String provider,
        String planType,
        int amount,
        String currency,
        String status,
        String createdAt,
        boolean retryable
) {
}
