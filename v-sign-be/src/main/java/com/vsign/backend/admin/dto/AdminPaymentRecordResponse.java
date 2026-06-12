package com.vsign.backend.admin.dto;

public record AdminPaymentRecordResponse(
        String transactionId,
        String userEmail,
        String planId,
        int amount,
        String currency,
        String status,
        String provider,
        String createdAt,
        String updatedAt,
        String overrideReason
) {
}
