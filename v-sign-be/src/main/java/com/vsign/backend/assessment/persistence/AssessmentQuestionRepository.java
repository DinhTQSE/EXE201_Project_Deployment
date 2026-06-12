package com.vsign.backend.assessment.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestionEntity, String> {
    List<AssessmentQuestionEntity> findByAssessmentIdOrderByOrderIndexAsc(String assessmentId);

    long countByAssessmentId(String assessmentId);
}
