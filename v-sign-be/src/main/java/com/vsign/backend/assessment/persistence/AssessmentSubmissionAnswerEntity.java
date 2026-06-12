package com.vsign.backend.assessment.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "assessment_submission_answers")
public class AssessmentSubmissionAnswerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "submission_id", nullable = false)
    private Integer submissionId;

    @Column(name = "question_id", nullable = false, length = 100)
    private String questionId;

    @Column(name = "selected_answer_id", length = 100)
    private String selectedAnswerId;

    @Column(nullable = false)
    private boolean correct;

    protected AssessmentSubmissionAnswerEntity() {
    }

    public AssessmentSubmissionAnswerEntity(Integer submissionId, String questionId, String selectedAnswerId, boolean correct) {
        this.submissionId = submissionId;
        this.questionId = questionId;
        this.selectedAnswerId = selectedAnswerId;
        this.correct = correct;
    }
}
