package com.vsign.backend.payment.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TierFeatureRepository extends JpaRepository<TierFeatureEntity, UUID> {
    List<TierFeatureEntity> findByTier_TierId(UUID tierId);
    List<TierFeatureEntity> findByTier_TitleIgnoreCase(String title);
}
