package com.complex.platform.floorplan;

import java.time.Instant;

public record FloorPlanImportTaskRecord(
        String id,
        String floorPlanId,
        String sourceFormat,
        String sourceName,
        String sourceHash,
        String sourcePayload,
        String status,
        Integer candidateCount,
        Integer errorCount,
        String operatorNote,
        Instant createdAt,
        Instant updatedAt
) {
}
