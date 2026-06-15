package com.complex.platform.integration;

import java.util.List;
import java.util.Map;

public record ReconciliationDifferenceRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String batchId,
        String providerCode,
        String businessType,
        String externalBusinessId,
        String differenceType,
        Long internalAmountCent,
        Long providerAmountCent,
        Long differenceAmountCent,
        String assignedToUserId,
        Map<String, Object> providerPayload
) {
}
