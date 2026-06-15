package com.complex.platform.amusement;

import java.time.Instant;

public record AmusementQueueEntryRecord(
        String id,
        String projectId,
        String sessionId,
        String customerId,
        String queueNo,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
