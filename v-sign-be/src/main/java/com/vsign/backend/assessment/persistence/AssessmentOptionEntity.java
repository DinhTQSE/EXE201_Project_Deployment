package com.vsign.backend.assessment.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "assessment_options")
public class AssessmentOptionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "question_id", nullable = false, length = 100)
    private String questionId;

    @Column(name = "option_id", nullable = false, length = 100)
    private String optionId;

    @Column(nullable = false, length = 160)
    private String text;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    protected AssessmentOptionEntity() {
    }

    public String getQuestionId() {
        return questionId;
    }

    public String getOptionId() {
        return optionId;
    }

    public String getText() {
        return text;
    }
}
