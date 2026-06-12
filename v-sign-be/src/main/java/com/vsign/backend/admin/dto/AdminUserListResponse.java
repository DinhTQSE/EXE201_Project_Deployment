package com.vsign.backend.admin.dto;

import java.util.List;

public record AdminUserListResponse(
        List<AdminUserResponse> users,
        int total
) {
}
