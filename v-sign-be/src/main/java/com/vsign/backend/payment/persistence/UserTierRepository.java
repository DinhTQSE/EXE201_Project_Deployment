package com.vsign.backend.payment.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface UserTierRepository extends JpaRepository<UserTierEntity, UUID> {

    @Query("""
        SELECT ut FROM UserTierEntity ut
        JOIN FETCH ut.tier t
        WHERE ut.user.id = :userId
          AND ut.isActive = true
          AND ut.deletedAt IS NULL
          AND ut.endTime > :now
        ORDER BY t.amount DESC
    """)
    List<UserTierEntity> findCurrentActiveByUserId(
            @Param("userId") UUID userId,
            @Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT ut FROM UserTierEntity ut
        JOIN FETCH ut.tier t
        WHERE ut.user.id = :userId
          AND ut.isActive = true
          AND ut.deletedAt IS NULL
          AND ut.endTime > :now
        ORDER BY t.amount DESC
    """)
    List<UserTierEntity> findCurrentActiveByUserIdForUpdate(
            @Param("userId") UUID userId,
            @Param("now") LocalDateTime now);

    @Query("""
        SELECT ut FROM UserTierEntity ut
        JOIN FETCH ut.tier t
        WHERE ut.isActive = true
          AND ut.deletedAt IS NULL
          AND ut.endTime <= :now
          AND t.amount > 0
    """)
    List<UserTierEntity> findExpiredPaidSubscriptions(@Param("now") LocalDateTime now);

    @Query("""
        SELECT DISTINCT ut.user.email FROM UserTierEntity ut
        WHERE ut.isActive = true
          AND ut.deletedAt IS NULL
          AND ut.endTime > :now
          AND ut.tier.amount > 0
    """)
    List<String> findActivePaidUserEmails(@Param("now") LocalDateTime now);
}
