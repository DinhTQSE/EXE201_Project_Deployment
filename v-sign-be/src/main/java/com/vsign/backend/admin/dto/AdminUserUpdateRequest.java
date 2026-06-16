package com.vsign.backend.admin.dto;

import jakarta.validation.constraints.Size;

public record AdminUserUpdateRequest(
        @Size(max = 120)
        String displayName,
        Boolean active,
        String accountType,
        String role,
        @Size(max = 500)
        String reason
) {
}
