package com.complex.platform.inventory.dto;

import java.util.List;
import java.util.Map;

public record StockInRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String sourceType,
        String sourceId,
        String warehouseId,
        List<Map<String, Object>> items
) {
}
