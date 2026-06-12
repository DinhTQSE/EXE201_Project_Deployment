package com.vsign.backend.learning.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "learning_units")
public class LearningUnitEntity {
    @Id
    @Column(name = "unit_id", nullable = false, length = 80)
    private String unitId;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "thumbnail_url", columnDefinition = "text")
    private String thumbnailUrl;

    @Column(name = "is_published", nullable = false)
    private boolean published;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    protected LearningUnitEntity() {
    }

    public String getUnitId() {
        return unitId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public boolean isPublished() {
        return published;
    }

    public int getOrderIndex() {
        return orderIndex;
    }
}
