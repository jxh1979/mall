package com.complex.platform.access.dto;

public record StaffLevelAssignmentRequest(
        String idempotencyKey,
        String operatorNote,
        String userId,
        String staffLevelId,
        Long baseSalaryAmountCent,
        String salaryCurrency,
        String salaryDetail
) {
}
