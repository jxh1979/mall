package com.complex.platform.access.staff.dto;

public record StaffPayrollCloseRequest(
        String idempotencyKey,
        String month
) {
}
