package com.vsign.backend.monetization.controller;

import com.vsign.backend.common.response.SuccessResponse;
import com.vsign.backend.common.security.JwtService;
import com.vsign.backend.monetization.dto.CheckoutIntentRequest;
import com.vsign.backend.monetization.dto.CheckoutIntentResponse;
import com.vsign.backend.monetization.dto.SubscriptionPlanResponse;
import com.vsign.backend.monetization.service.SubscriptionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {
    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/plans")
    public SuccessResponse<List<SubscriptionPlanResponse>> legacyPlans() {
        return SuccessResponse.ok("Subscription plans loaded", subscriptionService.legacyPlans());
    }

    @PostMapping("/checkout")
    public SuccessResponse<CheckoutIntentResponse> checkout(
            @Valid @RequestBody CheckoutIntentRequest request,
            @AuthenticationPrincipal JwtService.Principal principal
    ) {
        return SuccessResponse.ok("Checkout created", subscriptionService.createCheckout(request, principal.email()));
    }
}
