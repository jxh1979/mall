package com.complex.platform.bar;

import java.time.Instant;
import java.util.List;

public record BarReservationRecord(
        String id,
        String barId,
        String merchantId,
        String tableId,
        String customerId,
        Instant reservedAt,
        int guestCount,
        int allowedPassFreeGuestCount,
        long depositAmountCent,
        String depositStatus,
        String orderSource,
        String salesStaffId,
        String status,
        String createdTerminal,
        String deviceId,
        List<String> evidenceAttachmentIds,
        Instant createdAt,
        Instant updatedAt,
        String depositRule
) {
}
