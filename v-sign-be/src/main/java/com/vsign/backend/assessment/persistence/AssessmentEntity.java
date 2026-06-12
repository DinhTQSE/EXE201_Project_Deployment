package com.vsign.backend.assessment.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "assessments")
public class AssessmentEntity {
    @Id
    @Column(name = "assessment_id", nullable = false, length = 100)
    private String assessmentId;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(name = "passing_score", nullable = false)
    private int passingScore;

    @Column(name = "is_published", nullable = false)
    private boolean published;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    protected AssessmentEntity() {
    }

    public String getAssessmentId() {
        return assessmentId;
    }

    public String getTitle() {
        return title;
    }

    public int getPassingScore() {
        return passingScore;
    }

    public boolean isPublished() {
        return published;
    }
}
