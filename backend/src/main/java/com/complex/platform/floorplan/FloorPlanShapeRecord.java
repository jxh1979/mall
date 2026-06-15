package com.complex.platform.floorplan;

import java.time.Instant;
import java.util.List;

public record FloorPlanShapeRecord(
        String id,
        String floorPlanId,
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
    public FloorPlanShapeRecord {
        polygonPoints = polygonPoints == null ? List.of() : List.copyOf(polygonPoints);
    }
}
