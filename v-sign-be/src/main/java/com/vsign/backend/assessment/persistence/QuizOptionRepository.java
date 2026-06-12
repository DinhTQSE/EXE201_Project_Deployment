package com.vsign.backend.assessment.persistence;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizOptionRepository extends JpaRepository<QuizOptionEntity, Integer> {
    List<QuizOptionEntity> findByQuestionIdOrderByOrderIndexAsc(String questionId);

    List<QuizOptionEntity> findByQuestionIdInOrderByOrderIndexAsc(Collection<String> questionIds);
}
