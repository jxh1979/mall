package com.complex.platform.inventory;

import java.time.Instant;

public record InventoryRiskAlertRecord(
        String id,
        String alertType,
        String severity,
        String itemId,
        String locationId,
        String batchId,
        Integer availableQuantity,
        Integer thresholdQuantity,
        Instant expireAt,
        String status,
        String operatorUserId,
        Instant createdAt
) {
}
