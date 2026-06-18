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
        BigDecimal commissionRate,
        String depositRule
) {
    /** 向后兼容构造器（A4 前签名）：既有调用方零改动。 */
    public BarReservationRequest(
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
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, barId, tableId, reservationTime,
                guestCount, salespersonUserId, commissionRate, null);
    }
}
