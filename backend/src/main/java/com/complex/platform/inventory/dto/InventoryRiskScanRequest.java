package com.complex.platform.inventory.dto;

public record InventoryRiskScanRequest(
        String idempotencyKey,
        String operatorNote,
        Integer expiryWarningDays,
        String operatorUserId
) {
}
