package com.vsign.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewDecisionRequest(
        @NotBlank String decision,
        @NotBlank @Size(min = 10, max = 500) String reason
) {
}
