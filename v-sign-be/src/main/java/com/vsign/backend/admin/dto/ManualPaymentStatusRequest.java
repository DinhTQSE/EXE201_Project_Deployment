package com.vsign.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ManualPaymentStatusRequest(
        @NotBlank String status,
        @NotBlank @Size(min = 10, max = 500) String reason
) {
}
