package com.vsign.backend.monetization.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreatePaymentOrderRequest(
        @NotBlank
        @Pattern(regexp = "MOMO|ZALOPAY")
        String provider,

        String planId,

        @Pattern(regexp = "MONTHLY|YEARLY")
        String planType,

        @Min(49000)
        Integer amount
) {
}
