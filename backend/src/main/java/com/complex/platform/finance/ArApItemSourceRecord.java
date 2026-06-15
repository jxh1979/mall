package com.complex.platform.finance;

import java.time.Instant;

public record ArApItemSourceRecord(
        String id,
        String arApItemId,
        String sourceType,
        String sourceId,
        long amountCent,
        Instant createdAt
) {
}
