package com.vsign.backend.payment.service;

import com.vsign.backend.payment.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.model.webhooks.WebhookData;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayOSWebhookService {

    private final PayOSOrderRepository orderRepository;
    private final PayOSTransactionRepository transactionRepository;
    private final UserTierRepository userTierRepository;

    @Transactional
    public void handlePayOSWebhook(WebhookData data) {
        switch (data.getCode()) {
            case "00" -> handlePaid(data);
            case "01" -> handleCancelled(data);
            case "02" -> handleExpired(data);
            default   -> log.warn("Unknown PayOS webhook code: {}", data.getCode());
        }
    }

    private void handlePaid(WebhookData data) {
        PayOSOrderEntity order = lockOrder(data.getOrderCode());

        if (order.getStatus() == PaymentOrderStatus.PAID) {
            log.info("Duplicate PAID webhook for order {}, ignoring", order.getOrderCode());
            return;
        }

        long webhookAmount = data.getAmount() != null ? data.getAmount() : 0L;
        if (order.getAmount().longValue() != webhookAmount) {
            throw new IllegalStateException(
                    "PayOS amount mismatch: expected " + order.getAmount() + " got " + webhookAmount);
        }

        if (data.getPaymentLinkId() != null && order.getPaymentLinkId() != null
                && !data.getPaymentLinkId().equals(order.getPaymentLinkId())) {
            throw new IllegalStateException("PayOS paymentLinkId mismatch");
        }

        saveTransactionIfNew(order, data, PaymentTransactionStatus.SUCCESS);

        order.setStatus(PaymentOrderStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);

        upgradeUserTier(order);
    }

    private void handleCancelled(WebhookData data) {
        PayOSOrderEntity order = lockOrder(data.getOrderCode());
        if (order.getStatus() == PaymentOrderStatus.PAID
                || order.getStatus() == PaymentOrderStatus.CANCELLED) return;

        saveTransactionIfNew(order, data, PaymentTransactionStatus.FAILED);
        order.setStatus(PaymentOrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    private void handleExpired(WebhookData data) {
        PayOSOrderEntity order = lockOrder(data.getOrderCode());
        if (order.getStatus() == PaymentOrderStatus.PAID
                || order.getStatus() == PaymentOrderStatus.EXPIRED) return;

        saveTransactionIfNew(order, data, PaymentTransactionStatus.FAILED);
        order.setStatus(PaymentOrderStatus.EXPIRED);
        orderRepository.save(order);
    }

    private void upgradeUserTier(PayOSOrderEntity order) {
        var userId = order.getUser().getId();
        List<UserTierEntity> existing = userTierRepository
                .findCurrentActiveByUserIdForUpdate(userId, LocalDateTime.now());

        boolean hasPaid = existing.stream().anyMatch(ut -> ut.getTier().getAmount() > 0);
        if (hasPaid) {
            log.info("User {} already has active paid subscription, skipping upgrade", userId);
            return;
        }

        UserTierEntity userTier = new UserTierEntity();
        userTier.setUser(order.getUser());
        userTier.setTier(order.getTier());
        userTier.setStartTime(LocalDateTime.now());
        userTier.setEndTime(LocalDateTime.now().plusMonths(order.getTier().getNoMonth()));
        userTier.setIsActive(true);
        userTierRepository.save(userTier);
    }

    private PayOSOrderEntity lockOrder(long orderCode) {
        return orderRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderCode));
    }

    private void saveTransactionIfNew(PayOSOrderEntity order, WebhookData data,
                                       PaymentTransactionStatus status) {
        String ref = data.getReference();
        if (ref != null && transactionRepository.existsByReference(ref)) {
            log.info("Duplicate transaction reference {}, skipping", ref);
            return;
        }

        PayOSTransactionEntity tx = new PayOSTransactionEntity();
        tx.setPaymentOrder(order);
        tx.setAmount(data.getAmount() != null ? data.getAmount().intValue() : 0);
        tx.setPaymentLinkId(data.getPaymentLinkId());
        tx.setReference(ref);
        tx.setStatus(status);
        tx.setTransactionDate(LocalDateTime.now());
        transactionRepository.save(tx);
    }
}
