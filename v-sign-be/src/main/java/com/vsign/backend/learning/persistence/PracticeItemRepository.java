package com.vsign.backend.learning.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PracticeItemRepository extends JpaRepository<PracticeItemEntity, String> {
    List<PracticeItemEntity> findByPublishedTrueOrderByOrderIndexAsc();

    Optional<PracticeItemEntity> findByPracticeItemIdAndPublishedTrue(String practiceItemId);

    List<PracticeItemEntity> findByLessonIdAndPublishedTrueOrderByOrderIndexAsc(String lessonId);
}
