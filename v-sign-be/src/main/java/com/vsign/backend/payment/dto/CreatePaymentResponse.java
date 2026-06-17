package com.vsign.backend.payment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreatePaymentResponse {
    private String orderId;
    private Long orderCode;
    private String paymentLinkId;
    private String checkoutUrl;
    private String qrCode;
    private Integer amount;
    private String status;
    private String expiredAt;
}
