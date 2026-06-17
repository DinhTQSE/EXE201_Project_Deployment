package com.vsign.backend.payment.service;

import com.vsign.backend.payment.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentExpiryScheduler {

    private final PayOSOrderRepository orderRepository;
    private final UserTierRepository userTierRepository;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expirePendingOrders() {
        List<PayOSOrderEntity> expired = orderRepository.findExpiredPendingOrders(
                PaymentOrderStatus.PENDING, LocalDateTime.now());
        if (!expired.isEmpty()) {
            expired.forEach(o -> o.setStatus(PaymentOrderStatus.EXPIRED));
            orderRepository.saveAll(expired);
            log.info("Expired {} pending payment orders", expired.size());
        }
    }

    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void expireUserTierSubscriptions() {
        List<UserTierEntity> expired = userTierRepository.findExpiredPaidSubscriptions(LocalDateTime.now());
        if (!expired.isEmpty()) {
            expired.forEach(ut -> ut.setIsActive(false));
            userTierRepository.saveAll(expired);
            log.info("Deactivated {} expired paid subscriptions", expired.size());
        }
    }
}
