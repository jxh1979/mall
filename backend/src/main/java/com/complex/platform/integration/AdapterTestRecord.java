package com.complex.platform.integration;

import java.time.Instant;
import java.util.Map;

public record AdapterTestRecord(
        String id,
        String adapterId,
        String providerCode,
        String scenario,
        Map<String, Object> mockPayload,
        String testStatus,
        Map<String, Object> result,
        Instant createdAt
) {
}
