package com.vsign.backend.monetization.controller;

import com.vsign.backend.common.response.SuccessResponse;
import com.vsign.backend.common.security.JwtService;
import com.vsign.backend.monetization.dto.CreatePaymentOrderRequest;
import com.vsign.backend.monetization.dto.PaymentOrderResponse;
import com.vsign.backend.monetization.dto.PaymentStatusResponse;
import com.vsign.backend.monetization.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/orders")
    public SuccessResponse<PaymentOrderResponse> createOrder(
            @Valid @RequestBody CreatePaymentOrderRequest request,
            @AuthenticationPrincipal JwtService.Principal principal
    ) {
        return SuccessResponse.ok("Payment order created", paymentService.createOrder(request, principal.email()));
    }

    @GetMapping("/{transactionId}")
    public SuccessResponse<PaymentStatusResponse> status(
            @PathVariable String transactionId,
            @AuthenticationPrincipal JwtService.Principal principal
    ) {
        return SuccessResponse.ok("Payment status loaded", paymentService.status(transactionId, principal.email()));
    }
}
