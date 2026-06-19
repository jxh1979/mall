package com.complex.platform.inventory.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 采购单创建请求。
 *
 * <p>R5 T6：可选 {@code supplyContractId} 关联供应合同（采购周期卡控用，审批/下单步按合同
 * purchasable_scope 校验物料）。
 */
public record PurchaseRequestCreate(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String supplierId,
        List<Map<String, Object>> items,
        LocalDate expectedArrivalDate,
        String supplyContractId
) {
}
