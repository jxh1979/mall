package com.complex.platform.floorplan.dto;

import com.complex.platform.floorplan.FloorPlanPoint;
import java.util.List;

public record FloorPlanShapeSaveRequest(
        String idempotencyKey,
        String operatorNote,
        String shapeId,
        String shapeType,
        String refType,
        String refId,
        String label,
        List<FloorPlanPoint> polygonPoints,
        Integer zIndex,
        String status
) {
}
