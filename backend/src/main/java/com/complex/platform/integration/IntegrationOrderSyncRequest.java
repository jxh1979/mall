package com.complex.platform.integration;

import java.util.List;
import java.util.Map;

public record IntegrationOrderSyncRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String providerCode,
        String businessType,
        String syncType,
        String externalBusinessId,
        String status,
        Long amountCent,
        String currency,
        String channel,
        String externalChannelOrderId,
        Map<String, Object> providerPayload
) {
}
