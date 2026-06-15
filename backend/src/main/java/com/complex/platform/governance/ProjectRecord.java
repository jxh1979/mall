package com.complex.platform.governance;

import java.time.Instant;

public record ProjectRecord(
        String id,
        String merchantId,
        String venueType,
        String name,
        String code,
        String floor,
        Integer capacity,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
