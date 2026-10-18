package com.complex.platform.access.staff.dto;

// 文件版本：V0.01

/** 奖惩只发起复核，人力会签人自动解析，全部通过后才入账。 */
public record StaffPayrollAdjustmentRequest(
        String idempotencyKey,
        String userId,
        String month,
        String adjType,
        Long amountCent,
        String reason
) {
}
