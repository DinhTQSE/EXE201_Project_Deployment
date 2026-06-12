package com.vsign.backend.admin.dto;

public record ReviewQueueItemResponse(
        String contentId,
        String title,
        String contentType,
        String submittedBy,
        String status,
        String submittedAt,
        String reviewedBy,
        String reviewedAt,
        String reason
) {
}
