package com.complex.platform.ticketing.dto;

import java.util.List;

public record TicketRefundRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String targetId,
        Long refundAmountCent,
        String reason,
        String refundChannel
) {
}
