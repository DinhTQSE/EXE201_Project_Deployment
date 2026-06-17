package com.vsign.backend.payment.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TierRepository extends JpaRepository<TierEntity, UUID> {

    List<TierEntity> findByIsActiveTrueAndDeletedAtIsNull();

    Optional<TierEntity> findByTitleIgnoreCaseAndIsActiveTrueAndDeletedAtIsNull(String title);
}
