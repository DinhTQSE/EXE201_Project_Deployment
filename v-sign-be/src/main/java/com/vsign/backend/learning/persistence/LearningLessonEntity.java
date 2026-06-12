package com.vsign.backend.learning.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "learning_lessons")
public class LearningLessonEntity {
    @Id
    @Column(name = "lesson_id", nullable = false, length = 80)
    private String lessonId;

    @Column(name = "chapter_id", nullable = false, length = 80)
    private String chapterId;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "video_url", columnDefinition = "text")
    private String videoUrl;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "is_premium", nullable = false)
    private boolean premium;

    @Column(name = "is_published", nullable = false)
    private boolean published;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    protected LearningLessonEntity() {
    }

    public String getLessonId() {
        return lessonId;
    }

    public String getChapterId() {
        return chapterId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public boolean isPremium() {
        return premium;
    }

    public boolean isPublished() {
        return published;
    }

    public int getOrderIndex() {
        return orderIndex;
    }
}
