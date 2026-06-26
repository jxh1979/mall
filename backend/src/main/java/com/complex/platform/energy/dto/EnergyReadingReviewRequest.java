package com.complex.platform.energy.dto;

/** 复核异常抄表。 */
public record EnergyReadingReviewRequest(
        String idempotencyKey,
        String reviewedBy) {
}
