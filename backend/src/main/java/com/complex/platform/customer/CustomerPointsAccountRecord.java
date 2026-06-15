package com.complex.platform.customer;

import java.time.Instant;
import java.util.Map;

public record CustomerPointsAccountRecord(
        String id,
        String customerId,
        long balance,
        String level,
        Map<String, Object> recentSummary,
        Instant createdAt,
        Instant updatedAt
) {
}
