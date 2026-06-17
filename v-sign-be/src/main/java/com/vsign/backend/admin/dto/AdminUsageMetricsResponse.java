package com.vsign.backend.admin.dto;

import java.util.List;

public record AdminUsageMetricsResponse(
        String granularity,
        List<AdminUsagePointResponse> points
) {
}
