package com.vsign.backend.dictionary.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "dictionary_entries")
public class DictionaryEntryEntity {
    @Id
    private Integer id;

    @Column(nullable = false, length = 120)
    private String word;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(nullable = false, length = 20)
    private String difficulty;

    @Column(name = "difficulty_level", nullable = false)
    private int difficultyLevel;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "video_url", columnDefinition = "text")
    private String videoUrl;

    @Column(name = "thumbnail_url", columnDefinition = "text")
    private String thumbnailUrl;

    @Column(name = "is_published", nullable = false)
    private boolean published;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected DictionaryEntryEntity() {
    }

    public Integer getId() {
        return id;
    }

    public String getWord() {
        return word;
    }

    public String getCategory() {
        return category;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public String getDescription() {
        return description;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public boolean isPublished() {
        return published;
    }
}
