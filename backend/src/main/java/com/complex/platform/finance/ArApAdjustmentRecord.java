package com.complex.platform.finance;

import java.time.Instant;

/**
 * R5 T7：锁定后补录审批工单。approvalStatus ∈ pending / approved / rejected。
 * 仅 approved 才真正写入金额变动（见 FinanceService.approveAdjustment）。
 */
public record ArApAdjustmentRecord(
        String id,
        String arApItemId,
        String lockedDayCloseId,
        long adjustmentAmountCent,
        String approvalStatus,
        String requestedBy,
        String approvedBy,
        String reason,
        Instant createdAt,
        Instant decidedAt
) {
}
