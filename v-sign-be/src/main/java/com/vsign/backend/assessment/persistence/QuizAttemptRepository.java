package com.vsign.backend.assessment.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizAttemptRepository extends JpaRepository<QuizAttemptEntity, String> {
    boolean existsByUserKeyAndLessonIdAndSubmittedTrueAndPassedTrue(String userKey, String lessonId);
}
