package com.complex.platform.access.staff.dto;

public record StaffPayrollBaseRequest(
        String idempotencyKey,
        String userId,
        Long monthlySalaryCent
) {
}
