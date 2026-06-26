package com.complex.platform.contract.dto;

/** 变更审批（走同一并行链）：approverPermissionCode 定位当前阶段并行槽，decision=approve/reject。 */
public record ContractChangeApproveRequest(
        String idempotencyKey,
        String approverPermissionCode,
        String decision,
        String comment
) {
}
