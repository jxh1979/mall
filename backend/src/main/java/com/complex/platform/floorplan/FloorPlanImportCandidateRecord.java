package com.complex.platform.floorplan;

import java.time.Instant;
import java.util.List;

public record FloorPlanImportCandidateRecord(
        String id,
        String importTaskId,
        Integer candidateNo,
        String shapeType,
        String refType,
        String refId,
        String label,
        List<FloorPlanPoint> polygonPoints,
        Integer zIndex,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public FloorPlanImportCandidateRecord {
        polygonPoints = polygonPoints == null ? List.of() : List.copyOf(polygonPoints);
    }
}
