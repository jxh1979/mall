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
        Integer lossThreshold,
        // V152 内部结算价（F-TXN-05 + 通票赠饮结算）：分；可空；非空 >=0。运营在商品管理页录入。
        Long internalSettlementPriceCent,
        // V256 图形化点单 POS：分类 FK（可空=未分类）+ 商品图片相对路径（可空）。
        String categoryId,
        String imageUrl
) {
    /** 向后兼容构造器（V256 分类/图片前的 17 字段签名）：既有调用方零改动，分类/图片置空。 */
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
            Boolean stockDeductionRequired,
            Integer standardPourVolume,
            String pourVolumeUnit,
            Integer lossThreshold,
            Long internalSettlementPriceCent
    ) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, barId, productName, category, priceCent,
                costCent, skuCode, status, inventoryItemId, stockLocationId, stockDeductionRequired,
                standardPourVolume, pourVolumeUnit, lossThreshold, internalSettlementPriceCent, null, null);
    }

    /** 向后兼容构造器（不含按杯出酒量配置 REQ-22）：既有调用方无需改动，pour/内部价字段置空。 */
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
                null, null, null, null, null, null);
    }

    /** 向后兼容构造器（V152 内部结算价前的 16 字段签名）：既有调用方零改动，内部价置空。 */
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
            Boolean stockDeductionRequired,
            Integer standardPourVolume,
            String pourVolumeUnit,
            Integer lossThreshold
    ) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, barId, productName, category, priceCent,
                costCent, skuCode, status, inventoryItemId, stockLocationId, stockDeductionRequired,
                standardPourVolume, pourVolumeUnit, lossThreshold, null, null, null);
    }
}
