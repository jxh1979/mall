package com.complex.platform.inventory;

import java.time.Instant;
import java.util.List;

public record SavedBottleRecord(
        String id,
        String customerId,
        String barProjectId,
        String itemId,
        String bottleName,
        int initialQuantity,
        int remainingQuantity,
        String unit,
        Instant expireAt,
        String status,
        List<String> evidenceAttachmentIds,
        Instant createdAt,
        Instant updatedAt
) {
}
