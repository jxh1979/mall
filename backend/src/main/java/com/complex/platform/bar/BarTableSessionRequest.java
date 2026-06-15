package com.complex.platform.bar;

import java.time.LocalDate;
import java.util.List;

public record BarTableSessionRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String barId,
        String merchantId,
        LocalDate businessDate,
        String tableId,
        String reservationId,
        String customerId,
        Integer guestCount,
        String salesStaffId,
        String serviceStaffId,
        String openedByUserId
) {
}
