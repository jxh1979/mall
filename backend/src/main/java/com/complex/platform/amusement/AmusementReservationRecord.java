package com.complex.platform.amusement;

import java.time.Instant;

public record AmusementReservationRecord(
        String id,
        String projectId,
        String sessionId,
        String customerId,
        String reservationStatus,
        Instant createdAt,
        Instant updatedAt
) {
}
