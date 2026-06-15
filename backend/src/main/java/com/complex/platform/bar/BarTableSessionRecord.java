package com.complex.platform.bar;

import java.time.Instant;
import java.time.LocalDate;

public record BarTableSessionRecord(
        String id,
        String barId,
        String merchantId,
        LocalDate businessDate,
        String tableId,
        String reservationId,
        String customerId,
        int guestCount,
        long minConsumptionSnapshotCent,
        int allowedPassFreeGuestCountSnapshot,
        String salesStaffId,
        String serviceStaffId,
        String status,
        Instant openedAt,
        Instant closedAt,
        String sourceSessionId,
        long sessionVersion,
        Instant createdAt,
        Instant updatedAt
) {
}
