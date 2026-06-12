package com.vsign.backend.admin.controller;

import com.vsign.backend.admin.dto.AdminPaymentPageResponse;
import com.vsign.backend.admin.dto.AdminPaymentRecordResponse;
import com.vsign.backend.admin.dto.ManualPaymentStatusRequest;
import com.vsign.backend.admin.service.AdminPaymentService;
import com.vsign.backend.common.response.SuccessResponse;
import com.vsign.backend.common.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/payments")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminPaymentController {
    private final AdminPaymentService paymentService;

    public AdminPaymentController(AdminPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public SuccessResponse<AdminPaymentPageResponse> listPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return SuccessResponse.ok("Admin payments loaded", paymentService.listPayments(page, size));
    }

    @PatchMapping("/{transactionId}")
    public SuccessResponse<AdminPaymentRecordResponse> overrideStatus(
            @PathVariable String transactionId,
            @Valid @RequestBody ManualPaymentStatusRequest request,
            @AuthenticationPrincipal JwtService.Principal principal
    ) {
        return SuccessResponse.ok(
                "Payment status updated",
                paymentService.overrideStatus(transactionId, request, principal.email())
        );
    }
}
