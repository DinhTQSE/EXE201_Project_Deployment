package com.vsign.backend.monetization.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckoutIntentRepository extends JpaRepository<CheckoutIntentEntity, String> {
}
