package com.vsign.backend.learning.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LearningChapterRepository extends JpaRepository<LearningChapterEntity, String> {
    List<LearningChapterEntity> findByUnitIdAndPublishedTrueOrderByOrderIndexAsc(String unitId);

    long countByUnitIdAndPublishedTrue(String unitId);

    /** Single GROUP BY query replacing per-unit COUNT calls in listUnits(). */
    @Query("SELECT c.unitId, COUNT(c) FROM LearningChapterEntity c WHERE c.published = true GROUP BY c.unitId")
    List<Object[]> countPublishedGroupByUnitId();
}
