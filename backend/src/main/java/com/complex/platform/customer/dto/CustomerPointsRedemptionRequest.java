package com.complex.platform.customer.dto;

public record CustomerPointsRedemptionRequest(
        String idempotencyKey,
        String catalogItemId,
        Integer quantity,
        String sourceTerminal
) {
}
