package com.complex.platform.amusement;

import java.time.Instant;
import java.util.List;

public record AmusementSessionRecord(
        String id,
        String projectId,
        Instant startTime,
        Instant endTime,
        int capacity,
        int usedCapacity,
        String status,
        List<String> evidenceAttachmentIds,
        Instant createdAt,
        Instant updatedAt
) {
}
