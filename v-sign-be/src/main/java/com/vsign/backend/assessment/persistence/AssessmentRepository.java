package com.vsign.backend.assessment.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentRepository extends JpaRepository<AssessmentEntity, String> {
    List<AssessmentEntity> findByPublishedTrueOrderByOrderIndexAsc();

    Optional<AssessmentEntity> findByAssessmentIdAndPublishedTrue(String assessmentId);
}
