package com.vsign.backend.assessment.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "quiz_attempts")
public class QuizAttemptEntity {
    @Id
    @Column(name = "attempt_id", nullable = false, length = 120)
    private String attemptId;

    @Column(name = "quiz_id", nullable = false, length = 100)
    private String quizId;

    @Column(name = "lesson_id", nullable = false, length = 100)
    private String lessonId;

    @Column(name = "user_key", nullable = false, length = 255)
    private String userKey;

    @Column(nullable = false)
    private boolean submitted;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private boolean passed;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "timed_out", nullable = false)
    private boolean timedOut;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    protected QuizAttemptEntity() {
    }

    public QuizAttemptEntity(String attemptId, String quizId, String lessonId, String userKey) {
        this.attemptId = attemptId;
        this.quizId = quizId;
        this.lessonId = lessonId;
        this.userKey = userKey;
        this.submitted = false;
        this.score = 0;
        this.passed = false;
        this.durationSeconds = 0;
        this.timedOut = false;
    }

    public String getAttemptId() {
        return attemptId;
    }

    public String getQuizId() {
        return quizId;
    }

    public String getLessonId() {
        return lessonId;
    }

    public String getUserKey() {
        return userKey;
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public int getScore() {
        return score;
    }

    public boolean isPassed() {
        return passed;
    }

    public boolean isTimedOut() {
        return timedOut;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void submit(int score, boolean passed, int durationSeconds, boolean timedOut) {
        this.submitted = true;
        this.score = score;
        this.passed = passed;
        this.durationSeconds = durationSeconds;
        this.timedOut = timedOut;
        this.submittedAt = OffsetDateTime.now();
    }
}
