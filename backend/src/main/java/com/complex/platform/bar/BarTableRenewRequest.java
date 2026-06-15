package com.complex.platform.bar;

import java.time.Instant;
import java.util.List;

public record BarTableRenewRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        Instant renewedUntil,
        Long additionalMinConsumptionCent,
        Long additionalDepositAmountCent,
        Long additionalServiceFeeCent,
        String reason,
        String requestedByUserId,
        Long sessionVersion
) {
}
