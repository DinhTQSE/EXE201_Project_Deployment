package com.vsign.backend.assessment.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "quiz_questions")
public class QuizQuestionEntity {
    @Id
    @Column(name = "question_id", nullable = false, length = 100)
    private String questionId;

    @Column(name = "quiz_id", nullable = false, length = 100)
    private String quizId;

    @Column(nullable = false, length = 255)
    private String prompt;

    @Column(name = "correct_answer_id", nullable = false, length = 100)
    private String correctAnswerId;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    protected QuizQuestionEntity() {
    }

    public String getQuestionId() {
        return questionId;
    }

    public String getQuizId() {
        return quizId;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getCorrectAnswerId() {
        return correctAnswerId;
    }
}
