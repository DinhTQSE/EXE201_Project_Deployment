package com.vsign.backend.payment.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PayOSTransactionRepository extends JpaRepository<PayOSTransactionEntity, UUID> {

    boolean existsByReference(String reference);
}
