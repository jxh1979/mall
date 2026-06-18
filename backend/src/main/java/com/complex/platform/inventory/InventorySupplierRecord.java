package com.complex.platform.inventory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

public record InventorySupplierRecord(
        String id,
        String supplierName,
        String supplierType,
        String contactName,
        String contactPhone,
        String status,
        Instant createdAt,
        Instant updatedAt,
        @JsonIgnore String merchantId
) {
}
