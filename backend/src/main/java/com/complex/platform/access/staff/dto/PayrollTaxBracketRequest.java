package com.complex.platform.access.staff.dto;

// 文件版本：V0.01

/** 旧单档写请求形状仅供明确拒绝旧调用；入口返回 TAX_VERSION_REQUIRED，不写库或默认税目。 */
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
