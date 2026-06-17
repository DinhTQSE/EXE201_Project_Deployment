package com.vsign.backend.payment.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayOSOrderRepository extends JpaRepository<PayOSOrderEntity, UUID> {

    Optional<PayOSOrderEntity> findByOrderCode(Long orderCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM PayOSOrderEntity o WHERE o.orderCode = :orderCode")
    Optional<PayOSOrderEntity> findByOrderCodeForUpdate(@Param("orderCode") Long orderCode);

    List<PayOSOrderEntity> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    @Query("""
        SELECT o FROM PayOSOrderEntity o
        WHERE o.status = :status
          AND o.expiredAt <= :now
          AND o.deletedAt IS NULL
    """)
    List<PayOSOrderEntity> findExpiredPendingOrders(
            @Param("status") PaymentOrderStatus status,
            @Param("now") LocalDateTime now);
}
