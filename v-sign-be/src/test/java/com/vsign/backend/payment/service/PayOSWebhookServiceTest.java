package com.vsign.backend.payment.service;

import com.vsign.backend.auth.persistence.UserEntity;
import com.vsign.backend.common.mail.EmailService;
import com.vsign.backend.payment.persistence.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.payos.model.webhooks.WebhookData;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayOSWebhookServiceTest {

    @Mock PayOSOrderRepository orderRepository;
    @Mock PayOSTransactionRepository transactionRepository;
    @Mock UserTierRepository userTierRepository;
    @Mock EmailService emailService;

    @InjectMocks PayOSWebhookService service;

    @Test
    void handlePayOSWebhook_paidCreatesSubscription() {
        TierEntity tier = tier(UUID.randomUUID(), "plus", 49000, 1);
        UserEntity user = user(UUID.randomUUID());
        PayOSOrderEntity order = pendingOrder(user, tier, 49000);

        when(orderRepository.findByOrderCodeForUpdate(order.getOrderCode()))
                .thenReturn(Optional.of(order));
        when(transactionRepository.existsByReference("REF001")).thenReturn(false);
        when(userTierRepository.findCurrentActiveByUserIdForUpdate(eq(user.getId()), any()))
                .thenReturn(Collections.emptyList());
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userTierRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.handlePayOSWebhook(data("00", order.getOrderCode(), 49000L, null, "REF001"));

        verify(userTierRepository).save(argThat(ut ->
                Boolean.TRUE.equals(ut.getIsActive()) && ut.getTier().equals(tier)));
        verify(orderRepository).save(argThat(o -> o.getStatus() == PaymentOrderStatus.PAID));
    }

    @Test
    void handlePayOSWebhook_paidIsIdempotentForDuplicatePaidWebhook() {
        TierEntity tier = tier(UUID.randomUUID(), "plus", 49000, 1);
        UserEntity user = user(UUID.randomUUID());
        PayOSOrderEntity order = paidOrder(user, tier, 49000);

        when(orderRepository.findByOrderCodeForUpdate(order.getOrderCode()))
                .thenReturn(Optional.of(order));

        service.handlePayOSWebhook(data("00", order.getOrderCode(), 49000L, null, "REF002"));

        verify(userTierRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void handlePayOSWebhook_rejectsAmountMismatch() {
        TierEntity tier = tier(UUID.randomUUID(), "plus", 49000, 1);
        UserEntity user = user(UUID.randomUUID());
        PayOSOrderEntity order = pendingOrder(user, tier, 49000);

        when(orderRepository.findByOrderCodeForUpdate(order.getOrderCode()))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() ->
                service.handlePayOSWebhook(data("00", order.getOrderCode(), 1000L, null, "REF003")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("amount");
    }

    @Test
    void handlePayOSWebhook_cancelDoesNotOverridePaidOrder() {
        TierEntity tier = tier(UUID.randomUUID(), "plus", 49000, 1);
        UserEntity user = user(UUID.randomUUID());
        PayOSOrderEntity order = paidOrder(user, tier, 49000);

        when(orderRepository.findByOrderCodeForUpdate(order.getOrderCode()))
                .thenReturn(Optional.of(order));

        service.handlePayOSWebhook(data("01", order.getOrderCode(), 0L, null, null));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void handlePayOSWebhook_expiredMarksPendingOrderExpired() {
        TierEntity tier = tier(UUID.randomUUID(), "plus", 49000, 1);
        UserEntity user = user(UUID.randomUUID());
        PayOSOrderEntity order = pendingOrder(user, tier, 49000);

        when(orderRepository.findByOrderCodeForUpdate(order.getOrderCode()))
                .thenReturn(Optional.of(order));
        lenient().when(transactionRepository.existsByReference(any())).thenReturn(false);
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.handlePayOSWebhook(data("02", order.getOrderCode(), 0L, null, null));

        verify(orderRepository).save(argThat(o -> o.getStatus() == PaymentOrderStatus.EXPIRED));
    }

    // ---- helpers ----

    private UserEntity user(UUID id) {
        UserEntity u = new UserEntity();
        u.setId(id);
        return u;
    }

    private TierEntity tier(UUID id, String title, int amount, int noMonth) {
        TierEntity t = new TierEntity();
        t.setTierId(id);
        t.setTitle(title);
        t.setAmount(amount);
        t.setNoMonth(noMonth);
        t.setLimitedToken(100);
        t.setIsActive(true);
        return t;
    }

    private PayOSOrderEntity pendingOrder(UserEntity user, TierEntity tier, int amount) {
        PayOSOrderEntity o = new PayOSOrderEntity();
        o.setUser(user);
        o.setTier(tier);
        o.setOrderCode(Math.abs(new Random().nextLong() % 1_000_000_000L) + 1);
        o.setAmount(amount);
        o.setStatus(PaymentOrderStatus.PENDING);
        return o;
    }

    private PayOSOrderEntity paidOrder(UserEntity user, TierEntity tier, int amount) {
        PayOSOrderEntity o = pendingOrder(user, tier, amount);
        o.setStatus(PaymentOrderStatus.PAID);
        o.setPaidAt(LocalDateTime.now());
        return o;
    }

    private WebhookData data(String code, long orderCode, Long amount,
                              String paymentLinkId, String reference) {
        WebhookData d = mock(WebhookData.class);
        when(d.getCode()).thenReturn(code);
        when(d.getOrderCode()).thenReturn(orderCode);
        lenient().when(d.getAmount()).thenReturn(amount);
        lenient().when(d.getPaymentLinkId()).thenReturn(paymentLinkId);
        lenient().when(d.getReference()).thenReturn(reference);
        return d;
    }
}
