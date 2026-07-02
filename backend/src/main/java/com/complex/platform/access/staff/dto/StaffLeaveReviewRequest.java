package com.complex.platform.access.staff.dto;

public record StaffLeaveReviewRequest(
        String idempotencyKey,
        String decision,
        String reviewNote
) {
}
