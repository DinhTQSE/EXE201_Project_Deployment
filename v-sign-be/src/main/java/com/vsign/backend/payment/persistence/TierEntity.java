package com.vsign.backend.payment.persistence;

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
@Table(name = "tier")
public class TierEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "tier_id", updatable = false, nullable = false)
    private UUID tierId;

    @Column(name = "title", nullable = false, length = 50)
    private String title;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "no_month", nullable = false)
    private Integer noMonth;

    @Column(name = "limited_token", nullable = false)
    private Integer limitedToken;

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
