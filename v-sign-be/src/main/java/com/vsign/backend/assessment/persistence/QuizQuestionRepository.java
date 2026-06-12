package com.vsign.backend.assessment.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestionEntity, String> {
    List<QuizQuestionEntity> findByQuizIdOrderByOrderIndexAsc(String quizId);
}
