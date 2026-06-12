package com.vsign.backend.learning.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "practice_items")
public class PracticeItemEntity {
    @Id
    @Column(name = "practice_item_id", nullable = false, length = 100)
    private String practiceItemId;

    @Column(name = "lesson_id", nullable = false, length = 80)
    private String lessonId;

    @Column(nullable = false, length = 60)
    private String category;

    @Column(nullable = false, length = 30)
    private String level;

    @Column(nullable = false, length = 160)
    private String label;

    @Column(name = "expected_gloss", nullable = false, length = 120)
    private String expectedGloss;

    @Column(name = "source_video_file", length = 120)
    private String sourceVideoFile;

    @Column(name = "video_url", columnDefinition = "text")
    private String videoUrl;

    @Column(name = "is_published", nullable = false)
    private boolean published;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    protected PracticeItemEntity() {
    }

    public String getPracticeItemId() {
        return practiceItemId;
    }

    public String getLessonId() {
        return lessonId;
    }

    public String getCategory() {
        return category;
    }

    public String getLevel() {
        return level;
    }

    public String getLabel() {
        return label;
    }

    public String getExpectedGloss() {
        return expectedGloss;
    }

    public String getSourceVideoFile() {
        return sourceVideoFile;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public boolean isPublished() {
        return published;
    }

    public int getOrderIndex() {
        return orderIndex;
    }
}
