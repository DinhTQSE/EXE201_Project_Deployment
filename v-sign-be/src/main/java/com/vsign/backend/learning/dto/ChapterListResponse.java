package com.vsign.backend.learning.dto;

import java.util.List;

public record ChapterListResponse(
        String unitId,
        List<ChapterSummaryResponse> chapters
) {
}
