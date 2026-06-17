package com.vsign.backend.payment.service;

import com.vsign.backend.auth.persistence.UserEntity;
import com.vsign.backend.auth.persistence.UserRepository;
import com.vsign.backend.common.mail.EmailService;
import com.vsign.backend.payment.config.PayOSConfig;
import com.vsign.backend.payment.dto.CreatePaymentRequest;
import com.vsign.backend.payment.dto.PayOSReturnRequest;
import com.vsign.backend.payment.persistence.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.payos.PayOS;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayOSPaymentServiceTest {

    @Mock UserRepository userRepository;
    @Mock TierRepository tierRepository;
    @Mock PayOSOrderRepository orderRepository;
    @Mock UserTierRepository userTierRepository;
    @Mock PayOS payOS;
    @Mock PayOSConfig payOSConfig;
    @Mock EmailService emailService;

    @InjectMocks PayOSPaymentService service;

    @Test
    void createPayOSCheckout_rejectsFreeTier() {
        UUID tierId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UserEntity user = user(UUID.randomUUID());
        TierEntity free = tier(tierId, "free", 0);

        when(userRepository.findByEmailIgnoreCase("test@test.com")).thenReturn(Optional.of(user));
        when(tierRepository.findById(tierId)).thenReturn(Optional.of(free));

        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setTierId(tierId.toString());

        assertThatThrownBy(() -> service.createPayOSCheckout("test@test.com", req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createPayOSCheckout_rejectsWhenUserAlreadyHasPaidSubscription() {
        UUID tierId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UserEntity user = user(UUID.randomUUID());
        TierEntity plus = tier(tierId, "plus", 49000);
        TierEntity paidTier = tier(UUID.randomUUID(), "plus", 49000);
        UserTierEntity activePaid = new UserTierEntity();
        activePaid.setTier(paidTier);

        when(userRepository.findByEmailIgnoreCase("test@test.com")).thenReturn(Optional.of(user));
        when(tierRepository.findById(tierId)).thenReturn(Optional.of(plus));
        when(userTierRepository.findCurrentActiveByUserId(eq(user.getId()), any()))
                .thenReturn(List.of(activePaid));

        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setTierId(tierId.toString());

        assertThatThrownBy(() -> service.createPayOSCheckout("test@test.com", req))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void handlePayOSReturn_rejectsOrderOwnedByDifferentUser() {
        UserEntity requesting = user(UUID.randomUUID());
        UserEntity owner = user(UUID.randomUUID());

        PayOSOrderEntity order = new PayOSOrderEntity();
        order.setUser(owner);
        order.setStatus(PaymentOrderStatus.PENDING);

        when(userRepository.findByEmailIgnoreCase("requester@test.com")).thenReturn(Optional.of(requesting));
        when(orderRepository.findByOrderCode(123L)).thenReturn(Optional.of(order));

        PayOSReturnRequest req = new PayOSReturnRequest();
        req.setOrderCode(123L);
        req.setCancel(false);
        req.setStatus("PENDING");

        assertThatThrownBy(() -> service.handlePayOSReturn("requester@test.com", req))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void handlePayOSReturn_marksCancelledForCancelTrue() {
        UserEntity owner = user(UUID.randomUUID());

        PayOSOrderEntity order = new PayOSOrderEntity();
        order.setUser(owner);
        order.setStatus(PaymentOrderStatus.PENDING);

        when(userRepository.findByEmailIgnoreCase("owner@test.com")).thenReturn(Optional.of(owner));
        when(orderRepository.findByOrderCode(456L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PayOSReturnRequest req = new PayOSReturnRequest();
        req.setOrderCode(456L);
        req.setCancel(true);
        req.setStatus("CANCELLED");

        var response = service.handlePayOSReturn("owner@test.com", req);

        assertThat(response.getResolvedStatus()).isEqualTo("CANCELLED");
        verify(orderRepository).save(argThat(o -> o.getStatus() == PaymentOrderStatus.CANCELLED));
    }

    @Test
    void handlePayOSReturn_doesNotUpgradeTierWhenAlreadyHasActivePaidSubscription() {
        UserEntity owner = user(UUID.randomUUID());
        TierEntity existingTier = tier(UUID.randomUUID(), "plus", 49000);
        UserTierEntity activePaid = new UserTierEntity();
        activePaid.setTier(existingTier);

        PayOSOrderEntity order = new PayOSOrderEntity();
        order.setUser(owner);
        order.setTier(existingTier);
        order.setAmount(49000);
        order.setOrderCode(789L);
        order.setStatus(PaymentOrderStatus.PENDING);

        when(userRepository.findByEmailIgnoreCase("owner@test.com")).thenReturn(Optional.of(owner));
        when(orderRepository.findByOrderCode(789L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        // User already has a paid active subscription -> upgradeUserTier returns early
        when(userTierRepository.findCurrentActiveByUserIdForUpdate(eq(owner.getId()), any()))
                .thenReturn(List.of(activePaid));

        PayOSReturnRequest req = new PayOSReturnRequest();
        req.setOrderCode(789L);
        req.setCancel(false);
        req.setStatus("PAID");

        service.handlePayOSReturn("owner@test.com", req);

        verify(userTierRepository, never()).save(any());
    }

    private UserEntity user(UUID id) {
        UserEntity u = new UserEntity();
        u.setId(id);
        return u;
    }

    private TierEntity tier(UUID id, String title, int amount) {
        TierEntity t = new TierEntity();
        t.setTierId(id);
        t.setTitle(title);
        t.setAmount(amount);
        t.setIsActive(true);
        t.setNoMonth(1);
        t.setLimitedToken(20);
        return t;
    }
}
