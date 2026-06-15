package com.complex.platform.ops.productionreadiness.dto;

import java.time.Instant;
import java.util.List;

public record ProductionReadinessCheckResponse(
        String id,
        String checkScope,
        String result,
        int blockerCount,
        List<ProductionReadinessItemResponse> blockers,
        Instant checkedAt
) {
}
