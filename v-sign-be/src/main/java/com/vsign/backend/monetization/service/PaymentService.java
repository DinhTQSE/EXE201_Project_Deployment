package com.vsign.backend.monetization.service;

import com.vsign.backend.common.exception.BusinessException;
import com.vsign.backend.common.exception.ErrorCode;
import com.vsign.backend.monetization.dto.CreatePaymentOrderRequest;
import com.vsign.backend.monetization.dto.PaymentOrderResponse;
import com.vsign.backend.monetization.dto.PaymentStatusResponse;
import com.vsign.backend.monetization.persistence.PaymentOrderEntity;
import com.vsign.backend.monetization.persistence.PaymentOrderRepository;
import com.vsign.backend.monetization.persistence.SubscriptionPlanEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PaymentService {
    private final SubscriptionService subscriptionService;
    private final PaymentOrderRepository paymentOrderRepository;
    private final String paymentPublicBaseUrl;

    public PaymentService(
            SubscriptionService subscriptionService,
            PaymentOrderRepository paymentOrderRepository,
            @Value("${app.payment.public-base-url:https://payments.example.invalid}") String paymentPublicBaseUrl
    ) {
        this.subscriptionService = subscriptionService;
        this.paymentOrderRepository = paymentOrderRepository;
        this.paymentPublicBaseUrl = cleanBaseUrl(paymentPublicBaseUrl);
    }

    @Transactional
    public PaymentOrderResponse createOrder(CreatePaymentOrderRequest request, String userEmail) {
        SubscriptionPlanEntity plan = request.planId() != null && !request.planId().isBlank()
                ? subscriptionService.requirePlanEntity(request.planId())
                : subscriptionService.requirePlanEntityByType(request.planType());
        int amount = request.amount() == null ? plan.getPrice() : request.amount();
        String transactionId = "txn-" + UUID.randomUUID();
        String providerTransactionId = request.provider() + "-" + UUID.randomUUID();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(5);
        PaymentOrderEntity order = paymentOrderRepository.save(new PaymentOrderEntity(
                transactionId,
                providerTransactionId,
                request.provider(),
                plan.getPlanId(),
                plan.getPlanType(),
                amount,
                plan.getCurrency(),
                "PENDING",
                "VSIGN|" + request.provider() + "|" + plan.getPlanType() + "|" + transactionId + "|" + amount,
                request.provider().toLowerCase() + "://payment/" + transactionId,
                expiresAt,
                paymentPublicBaseUrl + "/qr/" + transactionId,
                300,
                true,
                userEmail
        ));
        return toOrderResponse(order);
    }

    public PaymentStatusResponse status(String transactionId, String userEmail) {
        return paymentOrderRepository.findByTransactionIdAndUserEmail(transactionId, userEmail)
                .map(this::toStatusResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    public List<PaymentStatusResponse> history(String email) {
        return paymentOrderRepository.findAllByUserEmailOrderByCreatedAtDesc(email).stream()
                .map(this::toStatusResponse)
                .toList();
    }

    private PaymentOrderResponse toOrderResponse(PaymentOrderEntity order) {
        return new PaymentOrderResponse(
                order.getTransactionId(),
                order.getProviderTransactionId(),
                order.getProvider(),
                order.getPlanId(),
                order.getPlanType(),
                order.getAmount(),
                order.getCurrency(),
                order.getStatus(),
                order.getQrCodeData(),
                order.getDeepLink(),
                order.getExpiresAt().toString(),
                order.getQrCodeUrl(),
                order.getExpiresInSeconds()
        );
    }

    private PaymentStatusResponse toStatusResponse(PaymentOrderEntity order) {
        return new PaymentStatusResponse(
                order.getTransactionId(),
                order.getProviderTransactionId(),
                order.getProvider(),
                order.getPlanType(),
                order.getAmount(),
                order.getCurrency(),
                order.getStatus(),
                order.getCreatedAt().toString(),
                order.isRetryable()
        );
    }

    private String cleanBaseUrl(String value) {
        String baseUrl = value == null || value.isBlank() ? "https://payments.example.invalid" : value.trim();
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
