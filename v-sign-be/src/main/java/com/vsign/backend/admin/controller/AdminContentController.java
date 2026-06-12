package com.vsign.backend.admin.controller;

import com.vsign.backend.admin.dto.ReviewDecisionRequest;
import com.vsign.backend.admin.dto.ReviewQueueItemResponse;
import com.vsign.backend.admin.dto.ReviewQueueResponse;
import com.vsign.backend.admin.service.AdminContentReviewService;
import com.vsign.backend.common.response.SuccessResponse;
import com.vsign.backend.common.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/content/review-queue")
public class AdminContentController {
    private final AdminContentReviewService contentReviewService;

    public AdminContentController(AdminContentReviewService contentReviewService) {
        this.contentReviewService = contentReviewService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','CONTENT_REVIEWER')")
    public SuccessResponse<ReviewQueueResponse> listReviewQueue() {
        return SuccessResponse.ok("Review queue loaded", contentReviewService.listQueue());
    }

    @PatchMapping("/{contentId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public SuccessResponse<ReviewQueueItemResponse> decide(
            @PathVariable String contentId,
            @Valid @RequestBody ReviewDecisionRequest request,
            @AuthenticationPrincipal JwtService.Principal principal
    ) {
        return SuccessResponse.ok(
                "Review decision saved",
                contentReviewService.decide(contentId, request, principal.email())
        );
    }
}
