package com.complex.platform.access.staff.dto;

import java.time.LocalDate;

public record StaffLeaveRequest(
        String idempotencyKey,
        String leaveType,
        LocalDate startDate,
        LocalDate endDate,
        String reason
) {
}
