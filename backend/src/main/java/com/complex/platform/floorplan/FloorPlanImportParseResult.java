package com.complex.platform.floorplan;

import java.util.List;

public record FloorPlanImportParseResult(
        List<CandidateDraft> candidates,
        List<ErrorDraft> errors
) {

    public FloorPlanImportParseResult {
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public record CandidateDraft(
            String shapeType,
            String refType,
            String refId,
            String label,
            List<FloorPlanPoint> polygonPoints,
            Integer zIndex
    ) {
        public CandidateDraft {
            polygonPoints = polygonPoints == null ? List.of() : List.copyOf(polygonPoints);
        }
    }

    public record ErrorDraft(
            Integer lineNo,
            String fieldName,
            String errorCode,
            String errorMessage
    ) {
    }
}
