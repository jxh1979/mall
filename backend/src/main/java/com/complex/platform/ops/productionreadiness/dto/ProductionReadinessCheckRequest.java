package com.complex.platform.ops.productionreadiness.dto;

public record ProductionReadinessCheckRequest(
        String idempotencyKey,
        String checkScope,
        String operatorNote
) {
}
