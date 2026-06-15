package com.complex.platform.floorplan.dto;

public record FloorPlanCreateRequest(
        String idempotencyKey,
        String operatorNote,
        String projectId,
        String buildingCode,
        String floorNo,
        String planName,
        String imageUrl,
        Integer imageWidth,
        Integer imageHeight,
        String versionNo,
        String status
) {
}
