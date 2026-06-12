package com.vsign.backend.admin.service;

import com.vsign.backend.admin.dto.ReviewDecisionRequest;
import com.vsign.backend.admin.dto.ReviewQueueItemResponse;
import com.vsign.backend.admin.dto.ReviewQueueResponse;
import com.vsign.backend.admin.persistence.AdminReviewQueueEntity;
import com.vsign.backend.admin.persistence.AdminReviewQueueRepository;
import com.vsign.backend.common.exception.BusinessException;
import com.vsign.backend.common.exception.ErrorCode;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminContentReviewService {
    private final AdminAuditService auditService;
    private final AdminReviewQueueRepository reviewQueueRepository;

    public AdminContentReviewService(AdminAuditService auditService, AdminReviewQueueRepository reviewQueueRepository) {
        this.auditService = auditService;
        this.reviewQueueRepository = reviewQueueRepository;
    }

    public ReviewQueueResponse listQueue() {
        List<ReviewQueueItemResponse> items = reviewQueueRepository.findAllByOrderByContentIdAsc().stream()
                .map(this::toResponse)
                .toList();
        return new ReviewQueueResponse(items, items.size());
    }

    @Transactional
    public ReviewQueueItemResponse decide(String contentId, ReviewDecisionRequest request, String actorEmail) {
        AdminReviewQueueEntity current = reviewQueueRepository.findById(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        String status = normalizeDecision(request.decision());
        current.markReviewed(status, actorEmail, request.reason());
        AdminReviewQueueEntity updated = reviewQueueRepository.save(current);
        auditService.recordAction(actorEmail, "CONTENT_REVIEW_DECISION", "CONTENT", contentId, request.reason());
        return toResponse(updated);
    }

    public int pendingReviewCount() {
        return reviewQueueRepository.countByStatus("PENDING");
    }

    private String normalizeDecision(String decision) {
        String normalized = decision.toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "APPROVED", "REJECTED", "NEEDS_CHANGES" -> normalized;
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        };
    }

    private ReviewQueueItemResponse toResponse(AdminReviewQueueEntity item) {
        return new ReviewQueueItemResponse(
                item.getContentId(),
                item.getTitle(),
                item.getContentType(),
                item.getSubmittedBy(),
                item.getStatus(),
                item.getSubmittedAt().toString(),
                item.getReviewedBy(),
                item.getReviewedAt() == null ? null : item.getReviewedAt().toString(),
                item.getReason()
        );
    }
}
