package com.vsign.backend.auth.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 120)
        String fullName,

        @Size(max = 120)
        String displayName,

        @Pattern(regexp = "^$|^https?://.+", message = "Avatar URL must be an HTTP(S) URL")
        String avatarUrl,

        @Size(max = 300)
        String bio
) {
    public String resolvedName() {
        return displayName != null ? displayName : fullName;
    }
}
