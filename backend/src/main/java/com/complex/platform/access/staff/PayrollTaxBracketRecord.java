package com.complex.platform.access.staff;

/**
 * 个税税表档位（payroll_tax_brackets，V217）：劳务报酬预扣个税税表，按毛收入（分）分档。
 * 税额 = GREATEST(0, (毛收入*incomeBaseBps/10000 - flatDeductionCent)*rateBps/10000 - quickDeductionCent)。
 * 原写死在 closePayrollMonth 的 CASE WHEN，现落配置表；税法参数变更改配置不改代码。
 */
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
