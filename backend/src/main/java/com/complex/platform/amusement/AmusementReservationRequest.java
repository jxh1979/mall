package com.complex.platform.amusement;

public record AmusementReservationRequest(
        String idempotencyKey,
        String projectId,
        String sessionId,
        String customerId,
        String reservationStatus
) {
}
