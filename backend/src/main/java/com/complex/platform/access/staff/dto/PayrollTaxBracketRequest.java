package com.complex.platform.access.staff.dto;

/** 个税税表档位 upsert 请求（V217）：按 id 幂等覆盖一档；category 默认 labor_remuneration。 */
public record PayrollTaxBracketRequest(
        String id,
        String category,
        Integer seq,
        Long incomeFloorCent,
        Long incomeCeilingCent,
        Integer incomeBaseBps,
        Long flatDeductionCent,
        Integer rateBps,
        Long quickDeductionCent,
        java.time.LocalDate effectiveFrom,
        String idempotencyKey
) {
}
