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
        @JsonIgnore String merchantId,
        // 第十八批 I 段第 76 条（BSM:1065「按杯按标准出酒量扣库存、超出记损耗」）：按杯出品、退品回库与驳回重扣的
        // 出酒量与损耗量（毫升）；只动已开瓶余量、没开新瓶的流水 quantity 为 0。按瓶流水两列为空。
        Integer volumeMl,
        Integer lossVolumeMl
) {

    /** 按瓶流水（不带出酒量与损耗量）。 */
    public StockMovementRecord(String id, String movementType, String itemId, String sourceLocationId, String targetLocationId,
            int quantity, Long unitCostCent, Long totalCostCent, String businessType, String businessId, String traceCode,
            String operatorId, List<String> evidenceAttachmentIds, String status, Instant createdAt, String merchantId) {
        this(id, movementType, itemId, sourceLocationId, targetLocationId, quantity, unitCostCent, totalCostCent, businessType,
                businessId, traceCode, operatorId, evidenceAttachmentIds, status, createdAt, merchantId, null, null);
    }

    /**
     * I-2(V296)：成本列脱敏副本——成本=内部口径（2026-07-08 内部结算价拍板范式），
     * 无 inventory.cost.view 权限的响应把成本列置 null（店员端/终端不可见）。
     */
    public StockMovementRecord withoutCosts() {
        if (unitCostCent == null && totalCostCent == null) {
            return this;
        }
        return new StockMovementRecord(id, movementType, itemId, sourceLocationId, targetLocationId,
                quantity, null, null, businessType, businessId, traceCode, operatorId,
                evidenceAttachmentIds, status, createdAt, merchantId, volumeMl, lossVolumeMl);
    }
}
