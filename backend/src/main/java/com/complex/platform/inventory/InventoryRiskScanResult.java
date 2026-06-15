package com.complex.platform.inventory;

import java.util.List;

public record InventoryRiskScanResult(
        int generatedCount,
        List<InventoryRiskAlertRecord> alerts
) {
}
