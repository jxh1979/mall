package com.complex.platform.bar;

import java.util.List;

public record BarOrderBillCreateRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String paymentMode,
        Boolean minSpendFillRequired,
        Integer serviceFeeRateBp
) {
}
