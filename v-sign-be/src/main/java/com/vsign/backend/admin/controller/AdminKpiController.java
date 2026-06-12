package com.vsign.backend.admin.controller;

import com.vsign.backend.admin.dto.AdminKpiResponse;
import com.vsign.backend.admin.service.AdminContentReviewService;
import com.vsign.backend.admin.service.AdminPaymentService;
import com.vsign.backend.admin.service.AdminUserService;
import com.vsign.backend.common.response.SuccessResponse;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/kpis")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminKpiController {
    private final AdminPaymentService paymentService;
    private final AdminUserService userService;
    private final AdminContentReviewService contentReviewService;

    public AdminKpiController(
            AdminPaymentService paymentService,
            AdminUserService userService,
            AdminContentReviewService contentReviewService
    ) {
        this.paymentService = paymentService;
        this.userService = userService;
        this.contentReviewService = contentReviewService;
    }

    @GetMapping
    public SuccessResponse<AdminKpiResponse> kpis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return SuccessResponse.ok(
                "Admin KPIs loaded",
                paymentService.kpis(
                        fromDate,
                        toDate,
                        userService.activeUsers(),
                        userService.premiumUsers(),
                        contentReviewService.pendingReviewCount()
                )
        );
    }
}
