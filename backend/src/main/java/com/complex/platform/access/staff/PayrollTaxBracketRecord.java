package com.complex.platform.access.staff;

// 文件版本：V0.01

/** 旧单档税表的历史只读记录（V217）；本记录不再参与新工资计算。
 * 新税表与认定按 PayrollTaxVersion / PayrollTaxAssessment 的完整审批版本办理。 */
public record PayrollTaxBracketRecord(
        String id,
        String category,
        int seq,
        long incomeFloorCent,
        Long incomeCeilingCent,
        int incomeBaseBps,
        long flatDeductionCent,
        int rateBps,
        long quickDeductionCent,
        java.time.LocalDate effectiveFrom,
        String updatedBy,
        java.time.Instant updatedAt
) {
}
