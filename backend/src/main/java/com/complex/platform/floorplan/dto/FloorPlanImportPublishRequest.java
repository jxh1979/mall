package com.complex.platform.floorplan.dto;

import java.util.List;

public record FloorPlanImportPublishRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> candidateIds
) {
    public FloorPlanImportPublishRequest {
        candidateIds = candidateIds == null ? List.of() : List.copyOf(candidateIds);
    }
}
