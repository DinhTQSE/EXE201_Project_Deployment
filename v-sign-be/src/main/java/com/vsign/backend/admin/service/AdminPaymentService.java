package com.vsign.backend.admin.service;

import com.vsign.backend.admin.dto.AdminKpiResponse;
import com.vsign.backend.admin.dto.AdminPaymentPageResponse;
import com.vsign.backend.admin.dto.AdminPaymentRecordResponse;
import com.vsign.backend.admin.dto.ManualPaymentStatusRequest;
import com.vsign.backend.common.exception.BusinessException;
import com.vsign.backend.common.exception.ErrorCode;
import com.vsign.backend.common.mail.EmailService;
import com.vsign.backend.payment.persistence.PayOSOrderEntity;
import com.vsign.backend.payment.persistence.PayOSOrderRepository;
import com.vsign.backend.payment.persistence.PaymentOrderStatus;
import com.vsign.backend.payment.persistence.UserTierEntity;
import com.vsign.backend.payment.persistence.UserTierRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminPaymentService {
    private final AdminAuditService auditService;
    private final PayOSOrderRepository orderRepository;
    private final UserTierRepository userTierRepository;
    private final EmailService emailService;

    public AdminPaymentService(
            AdminAuditService auditService,
            PayOSOrderRepository orderRepository,
            UserTierRepository userTierRepository,
            EmailService emailService
    ) {
        this.auditService = auditService;
        this.orderRepository = orderRepository;
        this.userTierRepository = userTierRepository;
        this.emailService = emailService;
    }

    public AdminPaymentPageResponse listPayments(int page, int size) {
        int normalizedPage = Math.max(0, page);
        int normalizedSize = Math.max(1, size);
        List<AdminPaymentRecordResponse> all = orderRepository.findAll().stream()
                .sorted(Comparator.comparing(PayOSOrderEntity::getCreatedAt).reversed())
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
        PayOSOrderEntity current = null;
        if (transactionId != null) {
            String clean = transactionId.replaceAll("\\D+", "");
            if (!clean.isEmpty()) {
                try {
                    Long orderCode = Long.parseLong(clean);
                    current = orderRepository.findByOrderCode(orderCode).orElse(null);
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
            if (current == null) {
                try {
                    current = orderRepository.findById(UUID.fromString(transactionId)).orElse(null);
                } catch (IllegalArgumentException e) {
                    // Ignore
                }
            }
        }

        if (current == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        String statusStr = normalizeStatus(request.status());
        PaymentOrderStatus newStatus = PaymentOrderStatus.valueOf(statusStr);
        current.setStatus(newStatus);
        current.setDescription(request.reason());
        if (newStatus == PaymentOrderStatus.PAID) {
            current.setPaidAt(LocalDateTime.now());
            upgradeUserTier(current);
        }
        PayOSOrderEntity updated = orderRepository.save(current);
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
        List<PayOSOrderEntity> successfulPayments = orderRepository.findAll().stream()
                .filter(payment -> PaymentOrderStatus.PAID == payment.getStatus())
                .filter(payment -> withinRange(payment.getCreatedAt(), fromDate, toDate))
                .toList();
        long revenue = successfulPayments.stream()
                .mapToLong(PayOSOrderEntity::getAmount)
                .sum();
        return new AdminKpiResponse(successfulPayments.size(), revenue, activeUsers, premiumUsers, pendingReviews);
    }

    private void upgradeUserTier(PayOSOrderEntity order) {
        var userId = order.getUser().getId();
        List<UserTierEntity> existing = userTierRepository
                .findCurrentActiveByUserId(userId, LocalDateTime.now());

        boolean hasPaid = existing.stream().anyMatch(ut -> ut.getTier().getAmount() > 0);
        if (hasPaid) {
            return;
        }

        UserTierEntity userTier = new UserTierEntity();
        userTier.setUser(order.getUser());
        userTier.setTier(order.getTier());
        userTier.setStartTime(LocalDateTime.now());
        userTier.setEndTime(LocalDateTime.now().plusMonths(order.getTier().getNoMonth()));
        userTier.setIsActive(true);
        userTierRepository.save(userTier);

        try {
            emailService.sendPaymentSuccessEmail(
                    order.getUser().getEmail(),
                    order.getUser().getFullName(),
                    order.getTier().getTitle(),
                    order.getAmount(),
                    order.getOrderCode(),
                    userTier.getStartTime(),
                    userTier.getEndTime()
            );
        } catch (Exception e) {
            // Ignore email errors in manual admin status overrides
        }
    }

    private boolean withinRange(LocalDateTime createdAt, LocalDate fromDate, LocalDate toDate) {
        if (createdAt == null) {
            return false;
        }
        LocalDate createdDate = createdAt.toLocalDate();
        boolean afterStart = fromDate == null || !createdDate.isBefore(fromDate);
        boolean beforeEnd = toDate == null || !createdDate.isAfter(toDate);
        return afterStart && beforeEnd;
    }

    private String normalizeStatus(String status) {
        String normalized = status.toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "PENDING" -> "PENDING";
            case "PAID" -> "PAID";
            case "FAILED" -> "FAILED";
            case "CANCELLED", "CANCELED" -> "CANCELLED";
            case "EXPIRED" -> "EXPIRED";
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        };
    }

    private AdminPaymentRecordResponse toResponse(PayOSOrderEntity payment) {
        return new AdminPaymentRecordResponse(
                payment.getOrderCode() != null ? "txn-" + payment.getOrderCode() : (payment.getOrderId() != null ? payment.getOrderId().toString() : ""),
                payment.getUser() != null ? payment.getUser().getEmail() : "unknown",
                payment.getTier() != null ? payment.getTier().getTitle() : "unknown",
                payment.getAmount() != null ? payment.getAmount() : 0,
                "VND",
                payment.getStatus() != null ? payment.getStatus().name() : "PENDING",
                "PAYOS",
                payment.getCreatedAt() != null ? payment.getCreatedAt().toString() : "",
                payment.getUpdatedAt() != null ? payment.getUpdatedAt().toString() : "",
                payment.getDescription()
        );
    }
}
