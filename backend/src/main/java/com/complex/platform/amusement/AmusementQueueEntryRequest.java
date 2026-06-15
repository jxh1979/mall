package com.complex.platform.amusement;

public record AmusementQueueEntryRequest(
        String idempotencyKey,
        String projectId,
        String sessionId,
        String customerId,
        String queueNo,
        String status
) {
}
