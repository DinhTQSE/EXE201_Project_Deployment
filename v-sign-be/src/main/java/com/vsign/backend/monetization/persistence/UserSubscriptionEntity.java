package com.vsign.backend.monetization.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "user_subscriptions")
public class UserSubscriptionEntity {
    @Id
    private String email;

    @Column(name = "plan_type")
    private String planType;

    private String status;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    protected UserSubscriptionEntity() {
    }

    public UserSubscriptionEntity(
            String email,
            String planType,
            String status,
            OffsetDateTime startedAt,
            OffsetDateTime expiresAt
    ) {
        this.email = email;
        this.planType = planType;
        this.status = status;
        this.startedAt = startedAt;
        this.expiresAt = expiresAt;
    }

    public String getPlanType() {
        return planType;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }
}
