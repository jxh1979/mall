package com.complex.platform.inventory;

import java.time.Instant;
import java.util.List;

/** 流转单明细:物料+数量+随货溯源码+结算单价。V277。 */
public record InventoryTransferLineRecord(
        String id,
        String transferOrderId,
        String itemId,
        int quantity,
        List<String> traceCodes,
        long unitPriceCent,
        Instant createdAt
) {
}
