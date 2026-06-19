package com.complex.platform.inventory.dto;

/**
 * R5 T6 采购单审批请求。{@code decision} ∈ {@code approved}/{@code rejected}。
 *
 * <p>可选 {@code supplyContractId}：审批步绑定供应合同；提供时按合同 purchasable_scope 卡控物料是否可采购。
 */
public record PurchaseRequestApproveRequest(
        String idempotencyKey,
        String decision,
        String supplyContractId,
        String note
) {
}
