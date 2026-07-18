package com.complex.platform.inventory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.List;

public record StockMovementRecord(
        String id,
        String movementType,
        String itemId,
        String sourceLocationId,
        String targetLocationId,
        int quantity,
        // V296 成本台账：本笔流水成本（整数分）。入库时 unitCostCent 可由调用方携带进货单价作为输入，
        // 落库时由仓储层按移动加权口径统一定稿；NULL = 无成本基准（存量库存未录进货价，不编造数字）。
        Long unitCostCent,
        Long totalCostCent,
        String businessType,
        String businessId,
        String traceCode,
        String operatorId,
        List<String> evidenceAttachmentIds,
        String status,
        Instant createdAt,
        @JsonIgnore String merchantId
) {
}
