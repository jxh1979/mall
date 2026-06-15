package com.complex.platform.inventory;

import java.time.Instant;

public record InventorySupplierRecord(
        String id,
        String supplierName,
        String supplierType,
        String contactName,
        String contactPhone,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
