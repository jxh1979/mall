package com.complex.platform.bar;

import java.util.List;

public record BarProductionTicketTransitionRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String targetStatus,
        String stationCode,
        String operatorUserId,
        String reason
) {
}
