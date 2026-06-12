package com.vsign.backend.monetization.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlanEntity, String> {
    List<SubscriptionPlanEntity> findByLegacyVisibleTrueOrderByDisplayOrderAsc();

    List<SubscriptionPlanEntity> findByActiveTrueAndPlanIdNotOrderByDisplayOrderAsc(String planId);

    Optional<SubscriptionPlanEntity> findByPlanTypeAndActiveTrue(String planType);
}
