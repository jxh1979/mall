package com.complex.platform.bar;

/**
 * V115 退菜退酒审批决策请求：对 pending 流水回写 approved（通过）或 rejected（驳回）。
 * decision 取 approved/rejected；reason 为审批理由（可空）。
 */
public record BarOrderItemVoidDecisionRequest(
        String idempotencyKey,
        String decision,
        String reason
) {
}
