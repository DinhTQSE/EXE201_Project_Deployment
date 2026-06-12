package com.vsign.backend.monetization.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrderEntity, String> {
    List<PaymentOrderEntity> findAllByOrderByCreatedAtDesc();

    Optional<PaymentOrderEntity> findByTransactionIdAndUserEmail(String transactionId, String userEmail);

    List<PaymentOrderEntity> findAllByUserEmailOrderByCreatedAtDesc(String userEmail);
}
