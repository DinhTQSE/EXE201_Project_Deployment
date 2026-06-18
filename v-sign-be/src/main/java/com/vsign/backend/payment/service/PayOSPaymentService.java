package com.vsign.backend.payment.service;

import com.vsign.backend.auth.persistence.UserEntity;
import com.vsign.backend.auth.persistence.UserRepository;
import com.vsign.backend.common.mail.EmailService;
import com.vsign.backend.payment.config.PayOSConfig;
import com.vsign.backend.payment.dto.*;
import com.vsign.backend.payment.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayOSPaymentService {

    private final UserRepository userRepository;
    private final TierRepository tierRepository;
    private final PayOSOrderRepository orderRepository;
    private final UserTierRepository userTierRepository;
    private final PayOS payOS;
    private final PayOSConfig payOSConfig;
    private final EmailService emailService;

    @Transactional
    public CreatePaymentResponse createPayOSCheckout(String userEmail, CreatePaymentRequest request) {
        UserEntity user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UUID tierId = UUID.fromString(request.getTierId());
        TierEntity tier = tierRepository.findById(tierId)
                .orElseThrow(() -> new IllegalArgumentException("Tier not found"));

        if (!Boolean.TRUE.equals(tier.getIsActive()) || tier.getDeletedAt() != null) {
            throw new IllegalArgumentException("Tier is not available");
        }
        if (tier.getAmount() == null || tier.getAmount() <= 0) {
            throw new IllegalArgumentException("Cannot purchase free tier");
        }

        List<UserTierEntity> activeTiers = userTierRepository.findCurrentActiveByUserId(user.getId(), LocalDateTime.now());
        boolean hasHigherOrEqualPaid = activeTiers.stream()
                .filter(ut -> ut.getTier().getAmount() > 0)
                .anyMatch(ut -> ut.getTier().getAmount() >= tier.getAmount());
        if (hasHigherOrEqualPaid) {
            throw new IllegalStateException("User already has an active paid subscription of the same or higher tier");
        }

        long orderCode = generateOrderCode();
        String description = "VSIGN" + String.format("%06d", orderCode % 1_000_000);
        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(15);

        PayOSOrderEntity order = new PayOSOrderEntity();
        order.setUser(user);
        order.setTier(tier);
        order.setOrderCode(orderCode);
        order.setAmount(tier.getAmount());
        order.setDescription(description);
        order.setStatus(PaymentOrderStatus.PENDING);
        order.setExpiredAt(expiredAt);
        order = orderRepository.save(order);

        try {
            CreatePaymentLinkRequest payosReq = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount(tier.getAmount().longValue())
                    .description(description)
                    .returnUrl(payOSConfig.getReturnUrl())
                    .cancelUrl(payOSConfig.getCancelUrl())
                    .expiredAt(Instant.now().plus(15, ChronoUnit.MINUTES).getEpochSecond())
                    .build();

            CreatePaymentLinkResponse data = payOS.paymentRequests().create(payosReq);
            order.setPaymentLinkId(data.getPaymentLinkId());
            order.setCheckoutUrl(data.getCheckoutUrl());
            order.setQrCode(data.getQrCode());
            order = orderRepository.save(order);
        } catch (Exception e) {
            order.setStatus(PaymentOrderStatus.FAILED);
            orderRepository.save(order);
            throw new RuntimeException("PayOS checkout creation failed: " + e.getMessage(), e);
        }

        return CreatePaymentResponse.builder()
                .orderId(order.getOrderId().toString())
                .orderCode(order.getOrderCode())
                .paymentLinkId(order.getPaymentLinkId())
                .checkoutUrl(order.getCheckoutUrl())
                .qrCode(order.getQrCode())
                .amount(order.getAmount())
                .status(order.getStatus().name())
                .expiredAt(order.getExpiredAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    public List<PaymentHistoryResponse> getMyPaymentHistory(String userEmail) {
        UserEntity user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return orderRepository.findByUser_IdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toHistoryResponse)
                .collect(Collectors.toList());
    }

    public List<TierResponse> listActiveTiers() {
        return tierRepository.findByIsActiveTrueAndDeletedAtIsNull().stream()
                .map(t -> TierResponse.builder()
                        .tierId(t.getTierId().toString())
                        .title(t.getTitle())
                        .amount(t.getAmount())
                        .noMonth(t.getNoMonth())
                        .limitedToken(t.getLimitedToken())
                        .isActive(t.getIsActive())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public PayOSReturnResponse handlePayOSReturn(String userEmail, PayOSReturnRequest request) {
        UserEntity requestingUser = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        PayOSOrderEntity order = orderRepository.findByOrderCode(request.getOrderCode())
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!order.getUser().getId().equals(requestingUser.getId())) {
            throw new SecurityException("Order does not belong to this user");
        }

        PaymentOrderStatus current = order.getStatus();
        if (isTerminal(current)) {
            return PayOSReturnResponse.builder()
                    .orderCode(order.getOrderCode())
                    .resolvedStatus(current.name())
                    .message("Order already in terminal state")
                    .build();
        }

        PaymentOrderStatus resolved = resolveReturnStatus(request);
        if (resolved != null && resolved != current) {
            order.setStatus(resolved);
            if (resolved == PaymentOrderStatus.PAID) {
                order.setPaidAt(LocalDateTime.now());
            }
            orderRepository.save(order);
        }

        PaymentOrderStatus finalStatus = resolved != null ? resolved : current;
        if (finalStatus == PaymentOrderStatus.PAID) {
            upgradeUserTier(order);
        }
        return PayOSReturnResponse.builder()
                .orderCode(order.getOrderCode())
                .resolvedStatus(finalStatus.name())
                .message("OK")
                .build();
    }

    private void upgradeUserTier(PayOSOrderEntity order) {
        var userId = order.getUser().getId();
        List<UserTierEntity> existing = userTierRepository
                .findCurrentActiveByUserIdForUpdate(userId, LocalDateTime.now());

        List<UserTierEntity> activePaid = existing.stream()
                .filter(ut -> ut.getTier().getAmount() > 0)
                .toList();

        if (!activePaid.isEmpty()) {
            boolean isUpgrade = activePaid.stream()
                    .allMatch(ut -> order.getTier().getAmount() > ut.getTier().getAmount());
            if (isUpgrade) {
                for (UserTierEntity oldTier : activePaid) {
                    oldTier.setIsActive(false);
                    userTierRepository.save(oldTier);
                }
            } else {
                log.info("User {} already has active paid subscription of same or higher tier, skipping upgrade", userId);
                return;
            }
        }

        UserTierEntity userTier = new UserTierEntity();
        userTier.setUser(order.getUser());
        userTier.setTier(order.getTier());
        userTier.setStartTime(LocalDateTime.now());
        userTier.setEndTime(LocalDateTime.now().plusMonths(order.getTier().getNoMonth()));
        userTier.setIsActive(true);
        userTierRepository.save(userTier);

        emailService.sendPaymentSuccessEmail(
                order.getUser().getEmail(),
                order.getUser().getFullName(),
                order.getTier().getTitle(),
                order.getAmount(),
                order.getOrderCode(),
                userTier.getStartTime(),
                userTier.getEndTime()
        );
    }

    private PaymentOrderStatus resolveReturnStatus(PayOSReturnRequest req) {
        if (Boolean.TRUE.equals(req.getCancel()) || "CANCELLED".equalsIgnoreCase(req.getStatus())) {
            return PaymentOrderStatus.CANCELLED;
        }
        if ("PAID".equalsIgnoreCase(req.getStatus())) {
            return PaymentOrderStatus.PAID;
        }
        return null;
    }

    private boolean isTerminal(PaymentOrderStatus status) {
        return status == PaymentOrderStatus.PAID
                || status == PaymentOrderStatus.CANCELLED
                || status == PaymentOrderStatus.EXPIRED
                || status == PaymentOrderStatus.FAILED;
    }

    private PaymentHistoryResponse toHistoryResponse(PayOSOrderEntity o) {
        return PaymentHistoryResponse.builder()
                .orderId(o.getOrderId().toString())
                .orderCode(o.getOrderCode())
                .tierId(o.getTier().getTierId().toString())
                .tierTitle(o.getTier().getTitle())
                .amount(o.getAmount())
                .status(o.getStatus().name())
                .paymentLinkId(o.getPaymentLinkId())
                .createdAt(fmt(o.getCreatedAt()))
                .paidAt(fmt(o.getPaidAt()))
                .expiredAt(fmt(o.getExpiredAt()))
                .build();
    }

    private String fmt(LocalDateTime dt) {
        return dt != null ? dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    private long generateOrderCode() {
        return Math.abs(ThreadLocalRandom.current().nextLong() % 1_000_000_000L) + 1;
    }
}
