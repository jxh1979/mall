package com.complex.platform.inventory.dto;

import java.util.List;

public record InventorySupplierRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String supplierName,
        String contactName,
        String contactPhone,
        String status
) {
}
