package com.vsign.backend.assessment.persistence;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentOptionRepository extends JpaRepository<AssessmentOptionEntity, Integer> {
    List<AssessmentOptionEntity> findByQuestionIdOrderByOrderIndexAsc(String questionId);

    List<AssessmentOptionEntity> findByQuestionIdInOrderByOrderIndexAsc(Collection<String> questionIds);
}
