package com.complex.platform.inventory.dto;

/**
 * R5 T6 供应合同审批请求。{@code decision} ∈ {@code approved}/{@code rejected}。
 */
public record SupplyContractApproveRequest(
        String idempotencyKey,
        String decision,
        String reason
) {
}
