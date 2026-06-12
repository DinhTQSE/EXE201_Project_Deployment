package com.vsign.backend.auth.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        AuthUserResponse user
) {
    public record AuthUserResponse(
            String id,
            String email,
            String fullName,
            String displayName,
            String avatarUrl,
            String bio,
            String role,
            String accountType
    ) {
    }
}
