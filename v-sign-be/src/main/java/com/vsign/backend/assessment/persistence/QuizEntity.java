package com.vsign.backend.assessment.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "lesson_quizzes")
public class QuizEntity {
    @Id
    @Column(name = "quiz_id", nullable = false, length = 100)
    private String quizId;

    @Column(name = "lesson_id", nullable = false, length = 100)
    private String lessonId;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(name = "passing_score", nullable = false)
    private int passingScore;

    @Column(name = "xp_award", nullable = false)
    private int xpAward;

    @Column(name = "requires_premium", nullable = false)
    private boolean requiresPremium;

    @Column(name = "is_published", nullable = false)
    private boolean published;

    protected QuizEntity() {
    }

    public String getQuizId() {
        return quizId;
    }

    public String getLessonId() {
        return lessonId;
    }

    public int getPassingScore() {
        return passingScore;
    }

    public int getXpAward() {
        return xpAward;
    }

    public boolean isRequiresPremium() {
        return requiresPremium;
    }
}
