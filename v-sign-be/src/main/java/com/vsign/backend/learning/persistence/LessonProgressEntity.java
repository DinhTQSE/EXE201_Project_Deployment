package com.vsign.backend.learning.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "lesson_progress")
public class LessonProgressEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_key", nullable = false, length = 255)
    private String userKey;

    @Column(name = "lesson_id", nullable = false, length = 80)
    private String lessonId;

    @Column(name = "completion_pct", nullable = false)
    private int completionPct;

    @Column(name = "last_position_seconds", nullable = false)
    private int lastPositionSeconds;

    @Column(nullable = false, length = 40)
    private String phase;

    @Column(name = "current_question_index")
    private Integer currentQuestionIndex;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected LessonProgressEntity() {
    }

    public LessonProgressEntity(String userKey, String lessonId) {
        this.userKey = userKey;
        this.lessonId = lessonId;
        this.completionPct = 0;
        this.lastPositionSeconds = 0;
        this.phase = "VIDEO";
        this.status = "NOT_STARTED";
        this.updatedAt = OffsetDateTime.now();
    }

    public String getUserKey() {
        return userKey;
    }

    public String getLessonId() {
        return lessonId;
    }

    public int getCompletionPct() {
        return completionPct;
    }

    public int getLastPositionSeconds() {
        return lastPositionSeconds;
    }

    public String getPhase() {
        return phase;
    }

    public Integer getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void update(int completionPct, int lastPositionSeconds, String phase, Integer currentQuestionIndex, String status) {
        this.completionPct = completionPct;
        this.lastPositionSeconds = lastPositionSeconds;
        this.phase = phase;
        this.currentQuestionIndex = currentQuestionIndex;
        this.status = status;
        this.updatedAt = OffsetDateTime.now();
    }
}
