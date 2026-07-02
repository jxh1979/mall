package com.complex.platform.access.staff.dto;

public record StaffPayrollAdjustmentRequest(
        String idempotencyKey,
        String userId,
        String month,
        String adjType,
        Long amountCent,
        String reason
) {
}
