package com.vsign.backend.gamification.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GamificationProfileRepository extends JpaRepository<GamificationProfileEntity, String> {
    List<GamificationProfileEntity> findAllByOrderByTotalXpDesc();
}
