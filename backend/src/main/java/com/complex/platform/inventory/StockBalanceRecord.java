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
        @JsonIgnore String merchantId
) {
}
