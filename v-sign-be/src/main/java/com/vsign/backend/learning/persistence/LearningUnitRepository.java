package com.vsign.backend.learning.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningUnitRepository extends JpaRepository<LearningUnitEntity, String> {
    List<LearningUnitEntity> findByPublishedTrueOrderByOrderIndexAsc();

    List<LearningUnitEntity> findAllByOrderByOrderIndexAsc();
}
