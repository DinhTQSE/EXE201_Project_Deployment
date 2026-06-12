package com.vsign.backend.monetization.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "payment_orders")
public class PaymentOrderEntity {
    @Id
    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "provider_transaction_id")
    private String providerTransactionId;

    private String provider;

    @Column(name = "plan_id")
    private String planId;

    @Column(name = "plan_type")
    private String planType;

    private int amount;
    private String currency;
    private String status;

    @Column(name = "qr_code_data")
    private String qrCodeData;

    @Column(name = "deep_link")
    private String deepLink;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "qr_code_url")
    private String qrCodeUrl;

    @Column(name = "expires_in_seconds")
    private int expiresInSeconds;

    private boolean retryable;

    @Column(name = "user_email")
    private String userEmail;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "manual_reason")
    private String manualReason;

    protected PaymentOrderEntity() {
    }

    public PaymentOrderEntity(
            String transactionId,
            String providerTransactionId,
            String provider,
            String planId,
            String planType,
            int amount,
            String currency,
            String status,
            String qrCodeData,
            String deepLink,
            OffsetDateTime expiresAt,
            String qrCodeUrl,
            int expiresInSeconds,
            boolean retryable,
            String userEmail
    ) {
        this.transactionId = transactionId;
        this.providerTransactionId = providerTransactionId;
        this.provider = provider;
        this.planId = planId;
        this.planType = planType;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.qrCodeData = qrCodeData;
        this.deepLink = deepLink;
        this.expiresAt = expiresAt;
        this.qrCodeUrl = qrCodeUrl;
        this.expiresInSeconds = expiresInSeconds;
        this.retryable = retryable;
        this.userEmail = userEmail;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getProviderTransactionId() {
        return providerTransactionId;
    }

    public String getProvider() {
        return provider;
    }

    public String getPlanId() {
        return planId;
    }

    public String getPlanType() {
        return planType;
    }

    public int getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getStatus() {
        return status;
    }

    public String getQrCodeData() {
        return qrCodeData;
    }

    public String getDeepLink() {
        return deepLink;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    public int getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getManualReason() {
        return manualReason;
    }

    public void overrideStatus(String status, String reason) {
        this.status = status;
        this.manualReason = reason;
        this.retryable = "PENDING".equals(status);
        this.updatedAt = OffsetDateTime.now();
    }
}
