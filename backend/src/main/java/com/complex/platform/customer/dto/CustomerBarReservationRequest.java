package com.complex.platform.customer.dto;

import java.time.Instant;

public record CustomerBarReservationRequest(
        String idempotencyKey,
        String barId,
        String tableId,
        String tableType,
        Instant reservationTime,
        Integer guestCount,
        String contactPhone
) {
}
