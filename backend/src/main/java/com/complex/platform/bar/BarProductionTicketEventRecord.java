package com.complex.platform.bar;

import java.time.Instant;
import java.util.List;

public record BarProductionTicketEventRecord(
        String id,
        String ticketId,
        String orderId,
        String barId,
        String actionType,
        String fromStatus,
        String toStatus,
        String stationCode,
        String operatorUserId,
        String operatorNote,
        String idempotencyKey,
        List<String> evidenceAttachmentIds,
        Instant createdAt
) {
}
