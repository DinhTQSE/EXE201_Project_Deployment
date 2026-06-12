package com.vsign.backend.admin.dto;

import java.util.List;

public record ReviewQueueResponse(
        List<ReviewQueueItemResponse> items,
        int total
) {
}
