package com.vsign.backend.admin.dto;

public record AdminAuditLogResponse(
        String id,
        String actorEmail,
        String action,
        String targetType,
        String targetId,
        String reason,
        String createdAt
) {
}
