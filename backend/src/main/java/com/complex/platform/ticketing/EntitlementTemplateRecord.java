package com.complex.platform.ticketing;

import java.time.Instant;

public record EntitlementTemplateRecord(
        String id,
        String name,
        String entitlementType,
        int defaultTotalTimes,
        long defaultValueCent,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
