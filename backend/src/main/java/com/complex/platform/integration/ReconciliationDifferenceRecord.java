package com.complex.platform.integration;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ReconciliationDifferenceRecord(
        String id,
        String batchId,
        String providerCode,
        String businessType,
        String externalBusinessId,
        String differenceType,
        long internalAmountCent,
        long providerAmountCent,
        long differenceAmountCent,
        String status,
        String sourceDomain,
        String assignedToUserId,
        String resolutionNote,
        Map<String, Object> providerPayload,
        List<String> evidenceAttachmentIds,
        Instant createdAt,
        Instant updatedAt
) {
}
