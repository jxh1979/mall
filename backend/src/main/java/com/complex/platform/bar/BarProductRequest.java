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
        Boolean stockDeductionRequired,
        Integer standardPourVolume,
        String pourVolumeUnit,
        Integer lossThreshold
) {
    /** 向后兼容构造器（不含按杯出酒量配置 REQ-22）：既有调用方无需改动，pour 字段置空。 */
    public BarProductRequest(
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
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, barId, productName, category, priceCent,
                costCent, skuCode, status, inventoryItemId, stockLocationId, stockDeductionRequired,
                null, null, null);
    }
}
