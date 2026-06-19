package com.complex.platform.integration;

import java.time.Instant;
import java.util.Map;

public record IntegrationOrderSyncRecord(
        String id,
        String providerCode,
        String businessType,
        String syncType,
        String externalBusinessId,
        String status,
        long amountCent,
        String currency,
        String channel,
        String externalChannelOrderId,
        Map<String, Object> providerPayload,
        Instant createdAt,
        Instant updatedAt
) {
}
