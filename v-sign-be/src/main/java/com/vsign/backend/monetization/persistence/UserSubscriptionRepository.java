package com.vsign.backend.monetization.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscriptionEntity, String> {
}
