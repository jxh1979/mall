package com.complex.platform.ops.productionreadiness;

import java.time.Instant;
import java.util.UUID;

public record ProductionReadinessCheck(
        UUID id,
        String checkScope,
        String result,
        int blockerCount,
        String operatorNote,
        Instant checkedAt,
        String checkedBy,
        String idempotencyKey
) {
}
