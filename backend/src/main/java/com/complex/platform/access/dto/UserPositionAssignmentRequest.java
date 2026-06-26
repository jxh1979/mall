package com.complex.platform.access.dto;

public record UserPositionAssignmentRequest(
        String idempotencyKey,
        String operatorNote,
        String userId,
        String positionCode,
        String staffLevelId,
        Long baseSalaryAmountCent,
        String salaryCurrency,
        String salaryDetail
) {
}
