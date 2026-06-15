package com.complex.platform.merchantbilling.dto;

import java.time.LocalDate;
import java.util.List;

public record MerchantBillCreateRequest(
        String idempotencyKey,
        String operatorNote,
        String merchantId,
        String arApItemId,
        LocalDate billPeriodStart,
        LocalDate billPeriodEnd,
        LocalDate dueDate,
        Long totalAmountCent,
        String sourceType,
        String sourceId,
        List<MerchantBillLineRequest> lines
) {
}
