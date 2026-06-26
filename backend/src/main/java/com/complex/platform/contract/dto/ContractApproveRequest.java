package com.complex.platform.contract.dto;

/** 当前阶段并行槽的一次审批：approverPermissionCode 定位是哪个并行审批人；decision=approve/reject。 */
public record ContractApproveRequest(
        String idempotencyKey,
        String approverPermissionCode,
        String decision,
        String comment
) {
}
