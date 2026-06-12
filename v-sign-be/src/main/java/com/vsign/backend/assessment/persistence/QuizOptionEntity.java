package com.vsign.backend.assessment.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "quiz_options")
public class QuizOptionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "question_id", nullable = false, length = 100)
    private String questionId;

    @Column(name = "answer_id", nullable = false, length = 100)
    private String answerId;

    @Column(nullable = false, length = 160)
    private String text;

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    protected QuizOptionEntity() {
    }

    public String getQuestionId() {
        return questionId;
    }

    public String getAnswerId() {
        return answerId;
    }

    public String getText() {
        return text;
    }

    public String getVideoUrl() {
        return videoUrl;
    }
}
