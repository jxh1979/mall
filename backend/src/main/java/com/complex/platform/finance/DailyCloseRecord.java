package com.complex.platform.finance;

import java.time.Instant;
import java.time.LocalDate;

public record DailyCloseRecord(
        String id,
        LocalDate businessDate,
        String scopeType,
        String scopeId,
        boolean forceClose,
        String status,
        Instant lockedAt,
        Instant createdAt
) {
}
