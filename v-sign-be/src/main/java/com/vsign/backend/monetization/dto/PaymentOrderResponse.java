package com.vsign.backend.monetization.dto;

public record PaymentOrderResponse(
        String transactionId,
        String providerTransactionId,
        String provider,
        String planId,
        String planType,
        int amount,
        String currency,
        String status,
        String qrCodeData,
        String deepLink,
        String expiresAt,
        String qrCodeUrl,
        int expiresInSeconds
) {
}
