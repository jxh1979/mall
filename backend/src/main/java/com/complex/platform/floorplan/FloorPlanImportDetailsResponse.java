package com.complex.platform.floorplan;

import java.util.List;

public record FloorPlanImportDetailsResponse(
        FloorPlanImportTaskRecord task,
        List<FloorPlanImportCandidateRecord> candidates,
        List<FloorPlanImportErrorRecord> errors
) {
    public FloorPlanImportDetailsResponse {
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
        errors = errors == null ? List.of() : List.copyOf(errors);
    }
}
