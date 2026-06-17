package com.vsign.backend.learning.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "signature_attempt_logs")
public class SignatureAttemptLogEntity {
    @Id
    @Column(name = "attempt_id")
    private String attemptId;

    @Column(name = "user_key")
    private String userKey;

    @Column(name = "practice_item_id")
    private String practiceItemId;

    @Column(name = "user_story_id")
    private String userStoryId;

    @Column(name = "document_upload_id")
    private String documentUploadId;

    @Column(name = "signature_vector_hash")
    private String signatureVectorHash;

    @Column(name = "duration_ms")
    private long durationMs;

    @Column(name = "ai_status")
    private String aiStatus;

    @Column(name = "target_gloss")
    private String targetGloss;

    @Column(name = "predicted_gloss")
    private String predictedGloss;

    private Double confidence;

    @Column(name = "is_correct")
    private Boolean correct;

    @Column(name = "frames_processed")
    private Integer framesProcessed;

    @Column(name = "hands_detected_frames")
    private Integer handsDetectedFrames;

    @Column(name = "inference_ms")
    private Double inferenceMs;

    @Column(name = "model_version")
    private String modelVersion;

    @Column(name = "label_version")
    private String labelVersion;

    private String status;
    private int score;

    @Column(name = "feedback_codes")
    private String feedbackCodes;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    protected SignatureAttemptLogEntity() {
    }

    public SignatureAttemptLogEntity(
            String attemptId,
            String userKey,
            String practiceItemId,
            String userStoryId,
            String documentUploadId,
            String signatureVectorHash,
            long durationMs,
            String aiStatus,
            String targetGloss,
            String predictedGloss,
            Double confidence,
            Boolean correct,
            Integer framesProcessed,
            Integer handsDetectedFrames,
            Double inferenceMs,
            String modelVersion,
            String labelVersion,
            String status,
            int score,
            String feedbackCodes
    ) {
        this.attemptId = attemptId;
        this.userKey = userKey;
        this.practiceItemId = practiceItemId;
        this.userStoryId = userStoryId;
        this.documentUploadId = documentUploadId;
        this.signatureVectorHash = signatureVectorHash;
        this.durationMs = durationMs;
        this.aiStatus = aiStatus;
        this.targetGloss = targetGloss;
        this.predictedGloss = predictedGloss;
        this.confidence = confidence;
        this.correct = correct;
        this.framesProcessed = framesProcessed;
        this.handsDetectedFrames = handsDetectedFrames;
        this.inferenceMs = inferenceMs;
        this.modelVersion = modelVersion;
        this.labelVersion = labelVersion;
        this.status = status;
        this.score = score;
        this.feedbackCodes = feedbackCodes;
        this.createdAt = OffsetDateTime.now();
    }

    public String getUserKey() {
        return userKey;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
