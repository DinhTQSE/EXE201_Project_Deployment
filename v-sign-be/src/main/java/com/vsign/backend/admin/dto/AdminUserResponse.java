package com.vsign.backend.admin.dto;

public record AdminUserResponse(
        String id,
        String email,
        String displayName,
        String role,
        String status,
        String accountType,
        String createdAt
) {
}
