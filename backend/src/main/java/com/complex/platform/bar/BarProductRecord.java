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
        Long internalSettlementPriceCent
) {

    /** 向后兼容构造器（V152 内部结算价前签名）：既有调用方零改动，内部价默认 null（未录入）。 */
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
            String soldOutReason
    ) {
        this(id, barId, merchantId, productName, productType, priceCent, currency,
                inventoryItemId, stockLocationId, stockDeductionRequired, status,
                createdAt, updatedAt, standardPourVolume, pourVolumeUnit, lossThreshold,
                soldOut, soldOutAt, soldOutReason, null);
    }

    /** 向后兼容构造器（V091 沽清前签名）：既有调用方零改动，新字段默认未沽清。 */
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
            Integer lossThreshold
    ) {
        this(id, barId, merchantId, productName, productType, priceCent, currency,
                inventoryItemId, stockLocationId, stockDeductionRequired, status,
                createdAt, updatedAt, standardPourVolume, pourVolumeUnit, lossThreshold,
                false, null, null, null);
    }
}
