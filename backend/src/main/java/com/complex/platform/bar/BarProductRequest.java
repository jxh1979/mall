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
        // ⚠️ 批10/E(V380)起本字段已封写:内部结算价唯一可编辑处 = 内部结算 SKU 目录(草案→他人复核转正)。
        //   请求携带非空值一律 400(防"以为改好了"的无声空转);商品上的 internal_settlement_price_cent 只是目录价快照。
        Long internalSettlementPriceCent,
        // V256 图形化点单 POS：分类 FK（可空=未分类）+ 商品图片相对路径（可空）。
        String categoryId,
        String imageUrl,
        // 批10/E(V380)：内部结算 SKU 目录引用(internal_price_catalog.item_code,可空)。
        //   引用既有条目或先「就地新建」再引用;条目绑定商户必须 = 本商品所属酒吧(bar_id)。
        String catalogItemCode
) {
    /** 向后兼容构造器（批10/E 目录引用前的 19 字段签名）：既有调用方零改动，引用置空。 */
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
            Long internalSettlementPriceCent,
            String categoryId,
            String imageUrl
    ) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, barId, productName, category, priceCent,
                costCent, skuCode, status, inventoryItemId, stockLocationId, stockDeductionRequired,
                standardPourVolume, pourVolumeUnit, lossThreshold, internalSettlementPriceCent, categoryId,
                imageUrl, null);
    }
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
