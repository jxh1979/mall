package com.complex.platform.bar;

import java.util.List;

public record BarBillSettleRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String paymentId,
        Long settlementAmountCent,
        String paymentChannel
) {
}
