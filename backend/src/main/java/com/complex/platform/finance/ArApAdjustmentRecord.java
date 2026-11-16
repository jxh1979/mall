package com.complex.platform.finance;

import java.time.Instant;
import java.time.LocalDate;

/**
 * R5 T7：锁定后补录审批工单。approvalStatus ∈ pending / approved / rejected。
 * 仅 approved 才真正写入金额变动（第十八批 AL 段 245-R1 起由统一审批终审通过回写，见 FinanceService.lockedBackfillDecided）。
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
        LocalDate originalBusinessDate,
        // 第十八批 Z 段第 186 条 186-R1：补录类型 amount（金额调整）／payment（收付款补录）与收付款补录的收付方式（金额调整为空）；
        // 186-R2：refund（退款补录），收付方式为所退原收款的收付方式。
        String kind,
        String paymentMethod,
        // 第十八批 AL 段第 245 条 245-R1：统一审批复核单号；第 253 条 253-R1：原始凭证附件编号（第十八批裁决 56）；253-R2：责任人账号。
        String reviewId,
        java.util.List<String> evidenceAttachmentIds,
        String responsibleStaffId
) {
}
