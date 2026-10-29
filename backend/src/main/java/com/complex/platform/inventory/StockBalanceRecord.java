package com.complex.platform.inventory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

public record StockBalanceRecord(
        String id,
        String itemId,
        String locationId,
        int quantity,
        int reservedQuantity,
        int availableQuantity,
        Instant updatedAt,
        @JsonIgnore String merchantId,
        String warehouseId,
        // 第十八批 I 段第 76 条（ENUM:79 库存按商品规格记瓶数）：已开瓶余量（毫升），开新瓶才减瓶数；只由酒吧按容量扣库与退品回库写。
        int openedVolumeMl
) {
    public StockBalanceRecord(String id, String itemId, String locationId, int quantity, int reservedQuantity, int availableQuantity,
            Instant updatedAt, String merchantId, String warehouseId) {
        this(id, itemId, locationId, quantity, reservedQuantity, availableQuantity, updatedAt, merchantId, warehouseId, 0);
    }
}
