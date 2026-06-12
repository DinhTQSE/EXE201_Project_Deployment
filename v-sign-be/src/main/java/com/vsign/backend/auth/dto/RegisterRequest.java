package com.vsign.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(regexp = ".*[A-Z].*", message = "Password must contain an uppercase letter")
        @Pattern(regexp = ".*\\d.*", message = "Password must contain a number")
        String password,

        @Size(max = 120)
        String fullName,

        @Size(max = 120)
        String displayName
) {
    public String resolvedName() {
        return displayName != null ? displayName : fullName;
    }
}
