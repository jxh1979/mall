package com.complex.platform.floorplan;

import java.util.List;
import java.util.Optional;

public interface FloorPlanRepository {
    List<FloorPlanRecord> findAllFloorPlans();

    List<FloorPlanRecord> findAllFloorPlansByMerchant(String merchantId);

    Optional<FloorPlanRecord> findFloorPlanById(String id);

    FloorPlanRecord saveFloorPlan(FloorPlanRecord record);

    List<FloorPlanShapeRecord> findShapesByFloorPlanId(String floorPlanId);

    Optional<FloorPlanShapeRecord> findShapeById(String id);

    FloorPlanShapeRecord saveShape(FloorPlanShapeRecord record);

    FloorPlanImportTaskRecord saveImportTask(FloorPlanImportTaskRecord record);

    Optional<FloorPlanImportTaskRecord> findImportTaskById(String id);

    List<FloorPlanImportTaskRecord> findImportTasksByFloorPlanId(String floorPlanId);

    FloorPlanImportCandidateRecord saveImportCandidate(FloorPlanImportCandidateRecord record);

    Optional<FloorPlanImportCandidateRecord> findImportCandidateById(String id);

    List<FloorPlanImportCandidateRecord> findImportCandidatesByTaskId(String importTaskId);

    FloorPlanImportErrorRecord saveImportError(FloorPlanImportErrorRecord record);

    List<FloorPlanImportErrorRecord> findImportErrorsByTaskId(String importTaskId);
}
