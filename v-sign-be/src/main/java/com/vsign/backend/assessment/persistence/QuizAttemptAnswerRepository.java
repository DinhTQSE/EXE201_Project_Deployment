package com.vsign.backend.assessment.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizAttemptAnswerRepository extends JpaRepository<QuizAttemptAnswerEntity, Integer> {
    List<QuizAttemptAnswerEntity> findByAttemptIdOrderByIdAsc(String attemptId);

    void deleteByAttemptId(String attemptId);
}
