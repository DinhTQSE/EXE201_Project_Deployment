package com.vsign.backend.payment.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PayOSReturnRequest {
    private String code;
    private String id;
    private Boolean cancel;
    private String status;
    private Long orderCode;
}
