package com.vsign.backend.assessment.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "quiz_attempt_answers")
public class QuizAttemptAnswerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "attempt_id", nullable = false, length = 120)
    private String attemptId;

    @Column(name = "question_id", nullable = false, length = 100)
    private String questionId;

    @Column(name = "selected_answer_id", length = 100)
    private String selectedAnswerId;

    @Column(nullable = false)
    private boolean correct;

    protected QuizAttemptAnswerEntity() {
    }

    public QuizAttemptAnswerEntity(String attemptId, String questionId, String selectedAnswerId, boolean correct) {
        this.attemptId = attemptId;
        this.questionId = questionId;
        this.selectedAnswerId = selectedAnswerId;
        this.correct = correct;
    }

    public String getQuestionId() {
        return questionId;
    }

    public String getSelectedAnswerId() {
        return selectedAnswerId;
    }

    public boolean isCorrect() {
        return correct;
    }
}
