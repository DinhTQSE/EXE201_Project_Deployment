package com.vsign.backend.monetization.dto;

public record CheckoutIntentResponse(
        String checkoutId,
        String planId,
        String status,
        String checkoutUrl
) {
}
