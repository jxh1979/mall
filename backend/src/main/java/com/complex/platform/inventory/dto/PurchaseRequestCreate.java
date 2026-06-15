package com.complex.platform.inventory.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record PurchaseRequestCreate(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String supplierId,
        List<Map<String, Object>> items,
        LocalDate expectedArrivalDate
) {
}
