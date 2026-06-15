package com.complex.platform.inventory.dto;

import java.util.List;
import java.util.Map;

public record StockAdjustmentRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String adjustmentType,
        List<Map<String, Object>> items,
        String reason
) {
}
