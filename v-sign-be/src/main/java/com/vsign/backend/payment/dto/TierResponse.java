package com.vsign.backend.payment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TierResponse {
    private String tierId;
    private String title;
    private Integer amount;
    private Integer noMonth;
    private Integer limitedToken;
    private Boolean isActive;
}
