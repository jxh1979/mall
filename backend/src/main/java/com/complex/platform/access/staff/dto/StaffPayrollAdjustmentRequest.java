package com.complex.platform.access.staff.dto;

/** CG-12：奖惩（bonus / penalty）只是发起复核，hrApproverId 为 HR 会签人，复核全部通过后才入账。 */
public record StaffPayrollAdjustmentRequest(
        String idempotencyKey,
        String userId,
        String month,
        String adjType,
        Long amountCent,
        String reason,
        String hrApproverId
) {
}
