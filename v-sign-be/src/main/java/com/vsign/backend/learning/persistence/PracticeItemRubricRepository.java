package com.vsign.backend.learning.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PracticeItemRubricRepository extends JpaRepository<PracticeItemRubricEntity, Integer> {
    List<PracticeItemRubricEntity> findByPracticeItemIdOrderByOrderIndexAsc(String practiceItemId);
}
