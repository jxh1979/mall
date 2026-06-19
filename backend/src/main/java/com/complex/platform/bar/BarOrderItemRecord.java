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
        boolean returnToStock
) {

    /** 向后兼容构造器（V091 退菜退酒前签名）：既有调用方零改动，新字段默认未撤单。 */
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
                false, null, null, null, false);
    }
}
