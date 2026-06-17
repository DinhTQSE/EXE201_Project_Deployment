package com.vsign.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetCompleteRequest(
        @NotBlank
        String token,

        @NotBlank
        @Size(min = 8, message = "Mật khẩu tối thiểu phải từ 8 ký tự.")
        String newPassword,

        @NotBlank
        String confirmPassword
) {
}
