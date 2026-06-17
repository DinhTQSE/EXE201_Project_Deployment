package com.vsign.backend.admin.dto;

public record AdminUserDetailResponse(
        AdminUserResponse user,
        AdminUserActivityResponse activity
) {
}
