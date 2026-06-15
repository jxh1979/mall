package com.complex.platform.bar;

import java.time.Instant;
import java.util.List;

public record BarBillAdjustmentRecord(
        String id,
        String billId,
        String adjustmentType,
        long amountCent,
        long beforePayableAmountCent,
        long afterPayableAmountCent,
        String reason,
        String requestedByUserId,
        boolean approvalRequired,
        String status,
        String idempotencyKey,
        List<String> evidenceAttachmentIds,
        String arApItemId,
        String snapshotJson,
        Instant createdAt
) {
}
