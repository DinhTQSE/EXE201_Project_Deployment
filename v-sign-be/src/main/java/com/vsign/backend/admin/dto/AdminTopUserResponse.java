package com.vsign.backend.admin.dto;

public record AdminTopUserResponse(
        String email,
        String displayName,
        int activeSeconds
) {
}
