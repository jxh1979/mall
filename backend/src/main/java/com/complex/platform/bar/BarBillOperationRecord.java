package com.complex.platform.bar;

import java.time.Instant;
import java.util.List;

public record BarBillOperationRecord(
        String id,
        String operationType,
        String primaryBillId,
        String secondaryBillId,
        String generatedBillId,
        long amountCent,
        String reason,
        String requestedByUserId,
        String status,
        String idempotencyKey,
        List<String> evidenceAttachmentIds,
        String snapshotJson,
        Instant createdAt
) {
}
