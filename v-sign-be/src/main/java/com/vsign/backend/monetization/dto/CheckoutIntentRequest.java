package com.vsign.backend.monetization.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckoutIntentRequest(
        @NotBlank
        String planId,

        @NotBlank
        String userId,

        String successUrl,
        String cancelUrl
) {
}
