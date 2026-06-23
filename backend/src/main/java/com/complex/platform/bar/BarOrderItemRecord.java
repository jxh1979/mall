package com.complex.platform.bar;

import java.time.Instant;

public record BarOrderItemRecord(
        String id,
        String orderId,
        String productId,
        String productName,
        int quantity,
        long unitPriceCent,
        long amountCent,
        String deliveryStatus,
        Instant createdAt,
        Instant updatedAt,
        Integer actualPourVolume,
        long lossVolume,
        // V091 退菜退酒：撤单标记 + 原因/时点/责任人 + 是否回补库存。
        boolean voided,
        String voidReason,
        Instant voidedAt,
        String voidedByUserId,
        boolean returnToStock,
        // V153 赠菜/赠酒标记（F-TXN-05）：随单赠送项 → amount_cent=0 不计消费额，
        // 但按商品内部结算价 × 数量从该销售提成基数扣除（成本不计提成）。
        boolean isGift
) {

    /** 向后兼容构造器（V091 退菜退酒前签名）：既有调用方零改动，新字段默认未撤单、非赠送。 */
    public BarOrderItemRecord(
            String id,
            String orderId,
            String productId,
            String productName,
            int quantity,
            long unitPriceCent,
            long amountCent,
            String deliveryStatus,
            Instant createdAt,
            Instant updatedAt,
            Integer actualPourVolume,
            long lossVolume
    ) {
        this(id, orderId, productId, productName, quantity, unitPriceCent, amountCent, deliveryStatus,
                createdAt, updatedAt, actualPourVolume, lossVolume,
                false, null, null, null, false, false);
    }

    /**
     * 便捷构造器：基础下单签名 + 赠送标记（void 字段默认未撤单）。
     * 用于下单/加菜（按请求设赠送标记）与出品重建（透传既有 isGift）。
     */
    public BarOrderItemRecord(
            String id,
            String orderId,
            String productId,
            String productName,
            int quantity,
            long unitPriceCent,
            long amountCent,
            String deliveryStatus,
            Instant createdAt,
            Instant updatedAt,
            Integer actualPourVolume,
            long lossVolume,
            boolean isGift
    ) {
        this(id, orderId, productId, productName, quantity, unitPriceCent, amountCent, deliveryStatus,
                createdAt, updatedAt, actualPourVolume, lossVolume,
                false, null, null, null, false, isGift);
    }

    /** 向后兼容构造器（V153 赠菜标记前 = V091 退菜退酒签名）：既有调用方零改动，新字段默认非赠送。 */
    public BarOrderItemRecord(
            String id,
            String orderId,
            String productId,
            String productName,
            int quantity,
            long unitPriceCent,
            long amountCent,
            String deliveryStatus,
            Instant createdAt,
            Instant updatedAt,
            Integer actualPourVolume,
            long lossVolume,
            boolean voided,
            String voidReason,
            Instant voidedAt,
            String voidedByUserId,
            boolean returnToStock
    ) {
        this(id, orderId, productId, productName, quantity, unitPriceCent, amountCent, deliveryStatus,
                createdAt, updatedAt, actualPourVolume, lossVolume,
                voided, voidReason, voidedAt, voidedByUserId, returnToStock, false);
    }
}
