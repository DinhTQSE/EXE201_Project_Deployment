package com.vsign.backend.assessment.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "assessment_questions")
public class AssessmentQuestionEntity {
    @Id
    @Column(name = "question_id", nullable = false, length = 100)
    private String questionId;

    @Column(name = "assessment_id", nullable = false, length = 100)
    private String assessmentId;

    @Column(nullable = false, length = 255)
    private String prompt;

    @Column(name = "correct_answer_id", nullable = false, length = 100)
    private String correctAnswerId;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    protected AssessmentQuestionEntity() {
    }

    public String getQuestionId() {
        return questionId;
    }

    public String getAssessmentId() {
        return assessmentId;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getCorrectAnswerId() {
        return correctAnswerId;
    }
}
