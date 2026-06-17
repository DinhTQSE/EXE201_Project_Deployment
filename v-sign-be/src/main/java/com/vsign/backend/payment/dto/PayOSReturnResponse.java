package com.vsign.backend.payment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PayOSReturnResponse {
    private Long orderCode;
    private String resolvedStatus;
    private String message;
}
