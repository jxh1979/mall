package com.complex.platform.bar;

import java.time.Instant;
import java.util.List;

public record StockMovementSnapshot(
        String id,
        String movementType,
        String itemId,
        String sourceLocationId,
        String targetLocationId,
        int quantity,
        String businessType,
        String businessId,
        String traceCode,
        String operatorId,
        List<String> evidenceAttachmentIds,
        String status,
        Instant createdAt,
        // 第十八批 I 段第 76 条（BSM:1065「按杯按标准出酒量扣库存、超出记损耗」）：按杯的出酒量与损耗量（毫升）；按瓶为空。
        Integer volumeMl,
        Integer lossVolumeMl
) {
    /** 按瓶流水（不带出酒量与损耗量）。 */
    public StockMovementSnapshot(String id, String movementType, String itemId, String sourceLocationId, String targetLocationId,
            int quantity, String businessType, String businessId, String traceCode, String operatorId,
            List<String> evidenceAttachmentIds, String status, Instant createdAt) {
        this(id, movementType, itemId, sourceLocationId, targetLocationId, quantity, businessType, businessId, traceCode, operatorId,
                evidenceAttachmentIds, status, createdAt, null, null);
    }
}
