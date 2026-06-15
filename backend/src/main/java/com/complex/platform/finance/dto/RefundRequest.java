package com.complex.platform.finance.dto;

import java.util.List;

public record RefundRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String targetId,
        Long refundAmountCent,
        String reason,
        String refundChannel
) {
}
