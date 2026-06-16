package com.vsign.backend.admin.controller;

import com.vsign.backend.admin.dto.AdminMetricsOverviewResponse;
import com.vsign.backend.admin.dto.AdminUsageMetricsResponse;
import com.vsign.backend.admin.service.AdminMetricsService;
import com.vsign.backend.common.response.SuccessResponse;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/metrics")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminMetricsController {
    private final AdminMetricsService metricsService;

    public AdminMetricsController(AdminMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/overview")
    public SuccessResponse<AdminMetricsOverviewResponse> overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return SuccessResponse.ok("Admin metrics loaded", metricsService.overview(fromDate, toDate));
    }

    @GetMapping("/usage")
    public SuccessResponse<AdminUsageMetricsResponse> usage(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "daily") String granularity
    ) {
        return SuccessResponse.ok("Admin usage metrics loaded", metricsService.usage(fromDate, toDate, granularity));
    }
}
