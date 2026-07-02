package com.complex.platform.access.staff.dto;

public record StaffCommissionRequest(
        String idempotencyKey,
        String userId,
        String month,
        String sourceType,
        String sourceId,
        Long amountCent,
        String note
) {
}
