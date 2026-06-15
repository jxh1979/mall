package com.complex.platform.bar;

import java.util.List;

public record OpenBarTableRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String reservationId,
        String tableId,
        String openedByUserId,
        Integer guestCount
) {
}
