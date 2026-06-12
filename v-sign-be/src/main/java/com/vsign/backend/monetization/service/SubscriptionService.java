package com.vsign.backend.monetization.service;

import com.vsign.backend.common.exception.BusinessException;
import com.vsign.backend.common.exception.ErrorCode;
import com.vsign.backend.monetization.dto.CheckoutIntentRequest;
import com.vsign.backend.monetization.dto.CheckoutIntentResponse;
import com.vsign.backend.monetization.dto.PlanListResponse;
import com.vsign.backend.monetization.dto.SubscriptionPlanResponse;
import com.vsign.backend.monetization.dto.SubscriptionSummaryResponse;
import com.vsign.backend.monetization.persistence.CheckoutIntentEntity;
import com.vsign.backend.monetization.persistence.CheckoutIntentRepository;
import com.vsign.backend.monetization.persistence.SubscriptionPlanEntity;
import com.vsign.backend.monetization.persistence.SubscriptionPlanRepository;
import com.vsign.backend.monetization.persistence.UserSubscriptionEntity;
import com.vsign.backend.monetization.persistence.UserSubscriptionRepository;
import java.time.temporal.ChronoUnit;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SubscriptionService {
    private static final String FREE_PLAN_ID = "free";

    private final SubscriptionPlanRepository planRepository;
    private final CheckoutIntentRepository checkoutIntentRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;

    public SubscriptionService(
            SubscriptionPlanRepository planRepository,
            CheckoutIntentRepository checkoutIntentRepository,
            UserSubscriptionRepository userSubscriptionRepository
    ) {
        this.planRepository = planRepository;
        this.checkoutIntentRepository = checkoutIntentRepository;
        this.userSubscriptionRepository = userSubscriptionRepository;
    }

    public List<SubscriptionPlanResponse> legacyPlans() {
        return planRepository.findByLegacyVisibleTrueOrderByDisplayOrderAsc().stream()
                .map(this::toPlanResponse)
                .toList();
    }

    public PlanListResponse activePlans() {
        return new PlanListResponse(activePlanList());
    }

    public List<SubscriptionPlanResponse> activePlanList() {
        return planRepository.findByActiveTrueAndPlanIdNotOrderByDisplayOrderAsc(FREE_PLAN_ID).stream()
                .map(this::toPlanResponse)
                .toList();
    }

    public SubscriptionPlanEntity requirePlanEntity(String planId) {
        return planRepository.findById(planId)
                .filter(SubscriptionPlanEntity::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    public SubscriptionPlanResponse requirePlan(String planId) {
        return toPlanResponse(requirePlanEntity(planId));
    }

    public SubscriptionPlanEntity requirePlanEntityByType(String planType) {
        if (planType == null || planType.isBlank()) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return planRepository.findByPlanTypeAndActiveTrue(planType)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    public SubscriptionPlanResponse requirePlanByType(String planType) {
        return toPlanResponse(requirePlanEntityByType(planType));
    }

    @Transactional
    public CheckoutIntentResponse createCheckout(CheckoutIntentRequest request, String userEmail) {
        SubscriptionPlanEntity plan = requirePlanEntity(request.planId());
        String checkoutId = "checkout-" + UUID.randomUUID();
        String checkoutUrl = "https://pay.vsign.test/checkout/" + plan.getPlanId();
        CheckoutIntentEntity checkout = checkoutIntentRepository.save(new CheckoutIntentEntity(
                checkoutId,
                request.planId(),
                userEmail,
                "CREATED",
                checkoutUrl,
                request.successUrl(),
                request.cancelUrl()
        ));
        return new CheckoutIntentResponse(
                checkout.getCheckoutId(),
                checkout.getPlanId(),
                checkout.getStatus(),
                checkout.getCheckoutUrl()
        );
    }

    @Transactional
    public SubscriptionSummaryResponse currentSubscription(String email) {
        UserSubscriptionEntity subscription = userSubscriptionRepository.findById(email)
                .orElseGet(() -> createDefaultSubscription(email));
        return toSubscriptionSummary(subscription);
    }

    private UserSubscriptionEntity createDefaultSubscription(String email) {
        return userSubscriptionRepository.save(new UserSubscriptionEntity(email, null, "FREE", null, null));
    }

    private SubscriptionSummaryResponse toSubscriptionSummary(UserSubscriptionEntity subscription) {
        int remainingDays = 0;
        if (subscription.getExpiresAt() != null) {
            long days = ChronoUnit.DAYS.between(OffsetDateTime.now().toLocalDate(), subscription.getExpiresAt().toLocalDate());
            remainingDays = (int) Math.max(0, days);
        }
        return new SubscriptionSummaryResponse(
                subscription.getPlanType(),
                subscription.getStatus(),
                subscription.getStartedAt() == null ? null : subscription.getStartedAt().toString(),
                subscription.getExpiresAt() == null ? null : subscription.getExpiresAt().toString(),
                remainingDays
        );
    }

    private SubscriptionPlanResponse toPlanResponse(SubscriptionPlanEntity plan) {
        return new SubscriptionPlanResponse(
                plan.getPlanId(),
                plan.getPlanType(),
                plan.getName(),
                plan.getAmount(),
                plan.getPrice(),
                plan.getCurrency(),
                plan.getDurationDays(),
                plan.isActive()
        );
    }
}
