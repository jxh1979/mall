package com.complex.platform.floorplan;

import java.time.Instant;

public record FloorPlanImportErrorRecord(
        String id,
        String importTaskId,
        Integer lineNo,
        String fieldName,
        String errorCode,
        String errorMessage,
        Instant createdAt
) {
}
