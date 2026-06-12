package com.vsign.backend.learning.dto;

import java.util.List;

public record PracticeItemsPageResponse(
        int page,
        int size,
        int totalElements,
        int totalPages,
        List<PracticeItemSummaryResponse> content
) {
}
