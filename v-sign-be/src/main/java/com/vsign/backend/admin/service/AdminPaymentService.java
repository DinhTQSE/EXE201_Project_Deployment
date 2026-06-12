package com.vsign.backend.admin.service;

import com.vsign.backend.admin.dto.AdminKpiResponse;
import com.vsign.backend.admin.dto.AdminPaymentPageResponse;
import com.vsign.backend.admin.dto.AdminPaymentRecordResponse;
import com.vsign.backend.admin.dto.ManualPaymentStatusRequest;
import com.vsign.backend.common.exception.BusinessException;
import com.vsign.backend.common.exception.ErrorCode;
import com.vsign.backend.monetization.persistence.PaymentOrderEntity;
import com.vsign.backend.monetization.persistence.PaymentOrderRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminPaymentService {
    private final AdminAuditService auditService;
    private final PaymentOrderRepository paymentOrderRepository;

    public AdminPaymentService(AdminAuditService auditService, PaymentOrderRepository paymentOrderRepository) {
        this.auditService = auditService;
        this.paymentOrderRepository = paymentOrderRepository;
    }

    public AdminPaymentPageResponse listPayments(int page, int size) {
        int normalizedPage = Math.max(0, page);
        int normalizedSize = Math.max(1, size);
        List<AdminPaymentRecordResponse> all = paymentOrderRepository.findAll().stream()
                .sorted(Comparator.comparing(PaymentOrderEntity::getTransactionId))
                .map(this::toResponse)
                .toList();
        List<AdminPaymentRecordResponse> pageItems = all.stream()
                .skip((long) normalizedPage * normalizedSize)
                .limit(normalizedSize)
                .toList();
        int totalPages = (int) Math.ceil((double) all.size() / normalizedSize);
        return new AdminPaymentPageResponse(pageItems, normalizedPage, normalizedSize, all.size(), totalPages);
    }

    @Transactional
    public AdminPaymentRecordResponse overrideStatus(
            String transactionId,
            ManualPaymentStatusRequest request,
            String actorEmail
    ) {
        PaymentOrderEntity current = paymentOrderRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        String status = normalizeStatus(request.status());
        current.overrideStatus(status, request.reason());
        PaymentOrderEntity updated = paymentOrderRepository.save(current);
        auditService.recordAction(actorEmail, "PAYMENT_STATUS_OVERRIDE", "PAYMENT", transactionId, request.reason());
        return toResponse(updated);
    }

    public AdminKpiResponse kpis(
            LocalDate fromDate,
            LocalDate toDate,
            int activeUsers,
            int premiumUsers,
            int pendingReviews
    ) {
        List<PaymentOrderEntity> successfulPayments = paymentOrderRepository.findAll().stream()
                .filter(payment -> "PAID".equals(payment.getStatus()))
                .filter(payment -> withinRange(payment.getCreatedAt(), fromDate, toDate))
                .toList();
        long revenue = successfulPayments.stream()
                .mapToLong(PaymentOrderEntity::getAmount)
                .sum();
        return new AdminKpiResponse(successfulPayments.size(), revenue, activeUsers, premiumUsers, pendingReviews);
    }

    private boolean withinRange(OffsetDateTime createdAt, LocalDate fromDate, LocalDate toDate) {
        LocalDate createdDate = createdAt.toLocalDate();
        boolean afterStart = fromDate == null || !createdDate.isBefore(fromDate);
        boolean beforeEnd = toDate == null || !createdDate.isAfter(toDate);
        return afterStart && beforeEnd;
    }

    private String normalizeStatus(String status) {
        String normalized = status.toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "PENDING", "PAID", "FAILED", "CANCELED", "REFUNDED" -> normalized;
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        };
    }

    private AdminPaymentRecordResponse toResponse(PaymentOrderEntity payment) {
        return new AdminPaymentRecordResponse(
                payment.getTransactionId(),
                payment.getUserEmail(),
                payment.getPlanId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getProvider(),
                payment.getCreatedAt().toString(),
                payment.getUpdatedAt().toString(),
                payment.getManualReason()
        );
    }
}
