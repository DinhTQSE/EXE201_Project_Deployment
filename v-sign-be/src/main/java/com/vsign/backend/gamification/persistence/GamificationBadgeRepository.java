package com.vsign.backend.gamification.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GamificationBadgeRepository extends JpaRepository<GamificationBadgeEntity, Integer> {
    List<GamificationBadgeEntity> findByUserIdOrderByIdAsc(String userId);
}
