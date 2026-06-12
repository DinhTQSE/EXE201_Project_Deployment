package com.vsign.backend.monetization.controller;

import com.vsign.backend.common.response.SuccessResponse;
import com.vsign.backend.monetization.dto.SubscriptionPlanResponse;
import com.vsign.backend.monetization.service.SubscriptionService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subscription")
public class PlanController {
    private final SubscriptionService subscriptionService;

    public PlanController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/plans")
    public SuccessResponse<List<SubscriptionPlanResponse>> plans() {
        return SuccessResponse.ok("Active plans loaded", subscriptionService.activePlanList());
    }
}
