package com.vsign.backend.gamification.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GamificationXpAwardRepository extends JpaRepository<GamificationXpAwardEntity, Integer> {
    boolean existsByUserIdAndEventId(String userId, String eventId);
}
