package com.vsign.backend.auth.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ActivityHeartbeatRequest(
        @Min(1)
        @Max(120)
        Integer activeSeconds
) {
}
