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
        Instant updatedAt
) {
}
