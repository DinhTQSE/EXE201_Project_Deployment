package com.vsign.backend.admin.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "admin_review_queue")
public class AdminReviewQueueEntity {
    @Id
    @Column(name = "content_id")
    private String contentId;

    private String title;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "submitted_by")
    private String submittedBy;

    private String status;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    private String reason;

    protected AdminReviewQueueEntity() {
    }

    public void markReviewed(String status, String reviewedBy, String reason) {
        this.status = status;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = OffsetDateTime.now();
        this.reason = reason;
    }

    public String getContentId() {
        return contentId;
    }

    public String getTitle() {
        return title;
    }

    public String getContentType() {
        return contentType;
    }

    public String getSubmittedBy() {
        return submittedBy;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public OffsetDateTime getReviewedAt() {
        return reviewedAt;
    }

    public String getReason() {
        return reason;
    }
}
