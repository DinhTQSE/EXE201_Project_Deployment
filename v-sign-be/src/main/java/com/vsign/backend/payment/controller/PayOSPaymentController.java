package com.vsign.backend.payment.controller;

import com.vsign.backend.common.security.JwtService;
import com.vsign.backend.payment.dto.*;
import com.vsign.backend.payment.service.PayOSPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PayOSPaymentController {

    private final PayOSPaymentService paymentService;

    @GetMapping("/tiers")
    public ResponseEntity<List<TierResponse>> tiers() {
        return ResponseEntity.ok(paymentService.listActiveTiers());
    }

    @PostMapping("/checkout")
    public ResponseEntity<CreatePaymentResponse> checkout(
            @Valid @RequestBody CreatePaymentRequest request,
            @AuthenticationPrincipal JwtService.Principal principal) {
        return ResponseEntity.ok(paymentService.createPayOSCheckout(principal.email(), request));
    }

    @GetMapping("/me")
    public ResponseEntity<List<PaymentHistoryResponse>> myHistory(
            @AuthenticationPrincipal JwtService.Principal principal) {
        return ResponseEntity.ok(paymentService.getMyPaymentHistory(principal.email()));
    }

    @PostMapping("/payos/return")
    public ResponseEntity<PayOSReturnResponse> payosReturn(
            @RequestBody PayOSReturnRequest request,
            @AuthenticationPrincipal JwtService.Principal principal) {
        return ResponseEntity.ok(paymentService.handlePayOSReturn(principal.email(), request));
    }
}
