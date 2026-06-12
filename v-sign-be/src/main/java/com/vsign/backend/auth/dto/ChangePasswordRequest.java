package com.vsign.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank
        String currentPassword,

        @NotBlank
        @Size(min = 8)
        @Pattern(regexp = ".*[A-Z].*", message = "Password must contain an uppercase letter")
        @Pattern(regexp = ".*\\d.*", message = "Password must contain a number")
        String newPassword
) {
}
