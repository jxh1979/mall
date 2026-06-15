package com.complex.platform.bar;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BarReservationRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String barId,
        String tableId,
        Instant reservationTime,
        Integer guestCount,
        String salespersonUserId,
        BigDecimal commissionRate
) {
}
