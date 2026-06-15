package com.complex.platform.inventory.dto;

import java.util.List;
import java.util.Map;

public record StockMovementRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String fromLocationId,
        String toLocationId,
        List<Map<String, Object>> items,
        String traceCode
) {
}
