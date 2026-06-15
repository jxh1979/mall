package com.complex.platform.floorplan.dto;

public record FloorPlanImportCreateRequest(
        String idempotencyKey,
        String operatorNote,
        String sourceFormat,
        String sourceName,
        String sourcePayload
) {
}
