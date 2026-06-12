package com.vsign.backend.admin.dto;

import java.util.List;

public record AdminPaymentPageResponse(
        List<AdminPaymentRecordResponse> payments,
        int page,
        int size,
        int totalElements,
        int totalPages
) {
}
