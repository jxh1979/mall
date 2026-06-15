package com.complex.platform.customer;

import java.time.Instant;

public record CustomerPointsLedgerRecord(
        String id,
        String customerId,
        long pointsDelta,
        long balanceAfter,
        String sourceType,
        String sourceId,
        String description,
        Instant createdAt
) {
}
