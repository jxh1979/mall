package com.complex.platform.merchantservice;

import java.time.Instant;
import java.util.List;

public record MerchantWorkOrderActionRecord(
        String id,
        String workOrderId,
        String actionType,
        String fromStatus,
        String toStatus,
        String operatorUserId,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        Instant createdAt
) {
    public MerchantWorkOrderActionRecord {
        evidenceAttachmentIds = evidenceAttachmentIds == null ? List.of() : List.copyOf(evidenceAttachmentIds);
    }
}
