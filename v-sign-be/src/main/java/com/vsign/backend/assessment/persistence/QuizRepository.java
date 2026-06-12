package com.vsign.backend.assessment.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<QuizEntity, String> {
    Optional<QuizEntity> findByLessonIdAndPublishedTrue(String lessonId);
}
