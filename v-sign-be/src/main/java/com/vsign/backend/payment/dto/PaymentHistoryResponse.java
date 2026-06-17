package com.vsign.backend.payment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentHistoryResponse {
    private String orderId;
    private Long orderCode;
    private String tierId;
    private String tierTitle;
    private Integer amount;
    private String status;
    private String paymentLinkId;
    private String createdAt;
    private String paidAt;
    private String expiredAt;
}
