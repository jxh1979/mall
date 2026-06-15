package com.complex.platform.integration;

import java.time.Instant;

public record OpenEndpointConfigRecord(
        String id,
        String endpointKey,
        String providerCode,
        String secretRef,
        String status,
        boolean webhookEnabled,
        boolean pollingCheckEnabled,
        Instant createdAt,
        Instant updatedAt
) {
}
