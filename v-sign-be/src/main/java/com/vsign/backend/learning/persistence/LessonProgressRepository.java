package com.vsign.backend.learning.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonProgressRepository extends JpaRepository<LessonProgressEntity, Integer> {
    Optional<LessonProgressEntity> findByUserKeyAndLessonId(String userKey, String lessonId);

    List<LessonProgressEntity> findByUserKeyAndLessonIdIn(String userKey, Collection<String> lessonIds);
}
