package com.vsign.backend.monetization.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "checkout_intents")
public class CheckoutIntentEntity {
    @Id
    @Column(name = "checkout_id")
    private String checkoutId;

    @Column(name = "plan_id")
    private String planId;

    @Column(name = "user_id")
    private String userId;

    private String status;

    @Column(name = "checkout_url")
    private String checkoutUrl;

    @Column(name = "success_url")
    private String successUrl;

    @Column(name = "cancel_url")
    private String cancelUrl;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    protected CheckoutIntentEntity() {
    }

    public CheckoutIntentEntity(
            String checkoutId,
            String planId,
            String userId,
            String status,
            String checkoutUrl,
            String successUrl,
            String cancelUrl
    ) {
        this.checkoutId = checkoutId;
        this.planId = planId;
        this.userId = userId;
        this.status = status;
        this.checkoutUrl = checkoutUrl;
        this.successUrl = successUrl;
        this.cancelUrl = cancelUrl;
        this.createdAt = OffsetDateTime.now();
    }

    public String getCheckoutId() {
        return checkoutId;
    }

    public String getPlanId() {
        return planId;
    }

    public String getStatus() {
        return status;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }
}
