package com.complex.platform.bar;

import java.time.Instant;

public record BarProductRecord(
        String id,
        String barId,
        String merchantId,
        String productName,
        String productType,
        long priceCent,
        String currency,
        String inventoryItemId,
        String stockLocationId,
        boolean stockDeductionRequired,
        String status,
        Instant createdAt,
        Instant updatedAt,
        Integer standardPourVolume,
        String pourVolumeUnit,
        Integer lossThreshold,
        // V091 沽清(86/估清)：售罄标记 + 时点 + 原因。
        boolean soldOut,
        Instant soldOutAt,
        String soldOutReason,
        // V152 内部结算价（F-TXN-05 + 通票赠饮结算）：分；可空（未录入=不参与结算/提成扣减）；非空 >=0。
        Long internalSettlementPriceCent,
        // V256 图形化点单 POS：可配置分类 FK（可空=未分类）+ 商品图片相对路径（可空，本地存储先例同 floor_plan）。
        String categoryId,
        String imageUrl,
        // V262 数据存档与审批后删除范式（照 V255 通票权益模板先例）。存档=软标记可恢复；删除=硬删走审批。
        Instant archivedAt,
        String archivedBy,
        String deleteStatus,
        String deleteRequestedBy,
        Instant deleteRequestedAt,
        String deleteReason
) {

    /** 内部结算价按权限脱敏:无 bar.internal_settlement_price.view 权限的读端返回副本内部价置 null。 */
    public BarProductRecord withoutInternalPrice() {
        return new BarProductRecord(id, barId, merchantId, productName, productType, priceCent, currency,
                inventoryItemId, stockLocationId, stockDeductionRequired, status, createdAt, updatedAt,
                standardPourVolume, pourVolumeUnit, lossThreshold, soldOut, soldOutAt, soldOutReason,
                null, categoryId, imageUrl, archivedAt, archivedBy, deleteStatus, deleteRequestedBy,
                deleteRequestedAt, deleteReason);
    }

    /** 向后兼容构造器（V262 存档/删除前签名）：既有调用方零改动，默认未存档 + delete_status=none。 */
    public BarProductRecord(
            String id,
            String barId,
            String merchantId,
            String productName,
            String productType,
            long priceCent,
            String currency,
            String inventoryItemId,
            String stockLocationId,
            boolean stockDeductionRequired,
            String status,
            Instant createdAt,
            Instant updatedAt,
            Integer standardPourVolume,
            String pourVolumeUnit,
            Integer lossThreshold,
            boolean soldOut,
            Instant soldOutAt,
            String soldOutReason,
            Long internalSettlementPriceCent,
            String categoryId,
            String imageUrl
    ) {
        this(id, barId, merchantId, productName, productType, priceCent, currency,
                inventoryItemId, stockLocationId, stockDeductionRequired, status,
                createdAt, updatedAt, standardPourVolume, pourVolumeUnit, lossThreshold,
                soldOut, soldOutAt, soldOutReason, internalSettlementPriceCent, categoryId, imageUrl,
                null, null, "none", null, null, null);
    }

    /** 向后兼容构造器（V256 分类/图片前签名）：既有调用方零改动，分类/图片默认 null。 */
    public BarProductRecord(
            String id,
            String barId,
            String merchantId,
            String productName,
            String productType,
            long priceCent,
            String currency,
            String inventoryItemId,
            String stockLocationId,
            boolean stockDeductionRequired,
            String status,
            Instant createdAt,
            Instant updatedAt,
            Integer standardPourVolume,
            String pourVolumeUnit,
            Integer lossThreshold,
            boolean soldOut,
            Instant soldOutAt,
            String soldOutReason,
            Long internalSettlementPriceCent
    ) {
        this(id, barId, merchantId, productName, productType, priceCent, currency,
                inventoryItemId, stockLocationId, stockDeductionRequired, status,
                createdAt, updatedAt, standardPourVolume, pourVolumeUnit, lossThreshold,
                soldOut, soldOutAt, soldOutReason, internalSettlementPriceCent, null, null);
    }

    // --- V262 存档/删除范式：不可变 record 的字段更新辅助（避免 28 参构造散落业务代码）。 ---
    public BarProductRecord withArchive(Instant archivedAt, String archivedBy, Instant updatedAt) {
        return new BarProductRecord(id, barId, merchantId, productName, productType, priceCent, currency,
                inventoryItemId, stockLocationId, stockDeductionRequired, status, createdAt, updatedAt,
                standardPourVolume, pourVolumeUnit, lossThreshold, soldOut, soldOutAt, soldOutReason,
                internalSettlementPriceCent, categoryId, imageUrl,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason);
    }

    public BarProductRecord withDelete(String deleteStatus, String deleteRequestedBy,
            Instant deleteRequestedAt, String deleteReason, Instant updatedAt) {
        return new BarProductRecord(id, barId, merchantId, productName, productType, priceCent, currency,
                inventoryItemId, stockLocationId, stockDeductionRequired, status, createdAt, updatedAt,
                standardPourVolume, pourVolumeUnit, lossThreshold, soldOut, soldOutAt, soldOutReason,
                internalSettlementPriceCent, categoryId, imageUrl,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason);
    }
}
