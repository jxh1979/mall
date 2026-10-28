package com.complex.platform.leasing.dto;

/**
 * 意向阶段推进请求（V161 重建）。targetStage 为 5 阶段之一，须逐级推进、不得向前跳级（第十八批第 30 条，PGM L500）。
 * 关键阶段（proposal_quoted/negotiation）送主管确认（PGM L502）：finalApproverId 为主线最终审批人，不填按发起人直接上级（PGM L540）。
 */
public record LeasingIntentAdvanceRequest(
        String targetStage,
        String finalApproverId,
        String idempotencyKey
) {
}
