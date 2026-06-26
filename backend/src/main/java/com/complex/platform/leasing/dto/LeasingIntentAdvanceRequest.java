package com.complex.platform.leasing.dto;

/**
 * 意向阶段推进请求（V161 重建）。targetStage 为 5 阶段之一。
 * 关键阶段（proposal_quoted/negotiation）须带 supervisorUserId（主管确认），否则阻断。
 */
public record LeasingIntentAdvanceRequest(
        String targetStage,
        String supervisorUserId,
        String idempotencyKey
) {
}
