package com.complex.platform.finance;

import java.time.Instant;
import java.time.LocalDate;

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
        Instant decidedAt,
        // 第十八批 R 段第 143 条 143-R4（ENUM:190「……并在补录记录上标明其所属的原营业日」）：台账单号与原营业日（锁定该台账的日结的营业日），
        // 查询时取自 ar_ap_items、finance_daily_closes，不另存列。
        String itemNo,
        LocalDate originalBusinessDate
) {
}
