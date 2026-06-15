package com.complex.platform.integration;

import java.time.Instant;
import java.util.Map;

public record IntegrationLedgerRecord(
        String id,
        String ledgerType,
        String providerCode,
        String businessId,
        String status,
        Map<String, Object> payload,
        Instant createdAt,
        Instant updatedAt
) {
}
