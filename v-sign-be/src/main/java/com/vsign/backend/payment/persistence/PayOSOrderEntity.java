package com.vsign.backend.payment.persistence;

import com.vsign.backend.auth.persistence.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payment_order")
public class PayOSOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id", updatable = false, nullable = false)
    private UUID orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uid", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tier_id", nullable = false)
    private TierEntity tier;

    @Column(name = "order_code", nullable = false, unique = true)
    private Long orderCode;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "payment_link_id")
    private String paymentLinkId;

    @Column(name = "checkout_url", length = 500)
    private String checkoutUrl;

    @Column(name = "qr_code", columnDefinition = "TEXT")
    private String qrCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private PaymentOrderStatus status = PaymentOrderStatus.PENDING;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
