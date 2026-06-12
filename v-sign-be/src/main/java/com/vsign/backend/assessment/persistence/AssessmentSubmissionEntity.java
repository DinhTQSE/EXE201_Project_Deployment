package com.vsign.backend.assessment.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "assessment_submissions")
public class AssessmentSubmissionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "assessment_id", nullable = false, length = 100)
    private String assessmentId;

    @Column(name = "user_id", nullable = false, length = 120)
    private String userId;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private boolean passed;

    @Column(name = "correct_answers", nullable = false)
    private int correctAnswers;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @Column(name = "awarded_xp", nullable = false)
    private int awardedXp;

    protected AssessmentSubmissionEntity() {
    }

    public AssessmentSubmissionEntity(String assessmentId, String userId, int score, boolean passed, int correctAnswers, int totalQuestions, int awardedXp) {
        this.assessmentId = assessmentId;
        this.userId = userId;
        this.score = score;
        this.passed = passed;
        this.correctAnswers = correctAnswers;
        this.totalQuestions = totalQuestions;
        this.awardedXp = awardedXp;
    }

    public Integer getId() {
        return id;
    }
}
