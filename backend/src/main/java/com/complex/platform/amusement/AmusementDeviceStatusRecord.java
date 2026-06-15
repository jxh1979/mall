package com.complex.platform.amusement;

import java.time.Instant;
import java.util.Map;

public record AmusementDeviceStatusRecord(
        String id,
        String projectId,
        String deviceId,
        String providerCode,
        String status,
        Instant lastSeenAt,
        Map<String, Object> rawStatus,
        Instant createdAt,
        Instant updatedAt
) {
}
