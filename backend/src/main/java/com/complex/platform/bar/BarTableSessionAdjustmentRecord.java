package com.complex.platform.bar;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record BarTableSessionAdjustmentRecord(
        String id,
        String sessionId,
        String adjustmentType,
        String sourceSessionId,
        String targetSessionId,
        String fromTableId,
        String toTableId,
        String barId,
        String merchantId,
        LocalDate businessDate,
        long minConsumptionBeforeCent,
        long minConsumptionAfterCent,
        Instant renewedUntil,
        long additionalMinConsumptionCent,
        long additionalDepositAmountCent,
        long additionalServiceFeeCent,
        String reason,
        String requestedByUserId,
        boolean approvalRequired,
        String status,
        String idempotencyKey,
        List<String> evidenceAttachmentIds,
        String snapshotJson,
        Instant createdAt
) {
}
