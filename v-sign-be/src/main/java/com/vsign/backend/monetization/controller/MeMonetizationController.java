package com.vsign.backend.monetization.controller;

import com.vsign.backend.common.response.SuccessResponse;
import com.vsign.backend.common.security.JwtService;
import com.vsign.backend.monetization.dto.PaymentStatusResponse;
import com.vsign.backend.monetization.dto.SubscriptionSummaryResponse;
import com.vsign.backend.monetization.service.PaymentService;
import com.vsign.backend.monetization.service.SubscriptionService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class MeMonetizationController {
    private final SubscriptionService subscriptionService;
    private final PaymentService paymentService;

    public MeMonetizationController(SubscriptionService subscriptionService, PaymentService paymentService) {
        this.subscriptionService = subscriptionService;
        this.paymentService = paymentService;
    }

    @GetMapping("/subscription")
    public SuccessResponse<SubscriptionSummaryResponse> subscription(
            @AuthenticationPrincipal JwtService.Principal principal
    ) {
        return SuccessResponse.ok("Subscription loaded", subscriptionService.currentSubscription(principal.email()));
    }

    @GetMapping("/payments")
    public SuccessResponse<List<PaymentStatusResponse>> payments(
            @AuthenticationPrincipal JwtService.Principal principal
    ) {
        return SuccessResponse.ok("Payment history loaded", paymentService.history(principal.email()));
    }
}
