package com.vsign.backend.learning.dto;

import java.util.List;

public record UnitListResponse(
        int page,
        int size,
        int totalElements,
        int totalPages,
        List<UnitSummaryResponse> units
) {
}
