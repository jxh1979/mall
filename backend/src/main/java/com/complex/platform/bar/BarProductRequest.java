package com.complex.platform.bar;

import java.util.List;

public record BarProductRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String barId,
        String productName,
        String category,
        Long priceCent,
        Long costCent,
        String skuCode,
        String status,
        String inventoryItemId,
        String stockLocationId,
        Boolean stockDeductionRequired
) {
}
