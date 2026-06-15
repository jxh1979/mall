package com.complex.platform.bar;

import java.time.Instant;
import java.util.List;

public record StockMovementSnapshot(
        String id,
        String movementType,
        String itemId,
        String sourceLocationId,
        String targetLocationId,
        int quantity,
        String businessType,
        String businessId,
        String traceCode,
        String operatorId,
        List<String> evidenceAttachmentIds,
        String status,
        Instant createdAt
) {
}
