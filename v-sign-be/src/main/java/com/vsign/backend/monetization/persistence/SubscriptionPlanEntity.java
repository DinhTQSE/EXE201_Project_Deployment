package com.vsign.backend.monetization.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "subscription_plans")
public class SubscriptionPlanEntity {
    @Id
    @Column(name = "plan_id")
    private String planId;

    @Column(name = "plan_type")
    private String planType;

    private String name;
    private int amount;
    private int price;
    private String currency;

    @Column(name = "duration_days")
    private int durationDays;

    private boolean active;

    @Column(name = "legacy_visible")
    private boolean legacyVisible;

    @Column(name = "display_order")
    private int displayOrder;

    protected SubscriptionPlanEntity() {
    }

    public String getPlanId() {
        return planId;
    }

    public String getPlanType() {
        return planType;
    }

    public String getName() {
        return name;
    }

    public int getAmount() {
        return amount;
    }

    public int getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isLegacyVisible() {
        return legacyVisible;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }
}
