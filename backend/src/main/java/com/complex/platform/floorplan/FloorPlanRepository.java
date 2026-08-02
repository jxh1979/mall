package com.complex.platform.floorplan;

import java.util.List;
import java.util.Optional;

public interface FloorPlanRepository {
    List<FloorPlanRecord> findAllFloorPlans();

    /**
     * 按综合体（project）scope 列出楼层平面图（mall_floor_plans.project_id = token scope）。
     * 商户面/综合体面数据：按 project_id 行级过滤。默认空，JdbcFloorPlanRepository 用 SQL 谓词下推。
     * 隔离闸口（AC-ISO-1）：提供 project_id 行级 scoped 读通道（与 CSV scope_source=project_id 对齐）。
     * 勿删:本方法零调用是常态(多综合体隔离通道,建成待激活),但 verify:tenant-isolation 门禁
     * 以「该表存在 scoped 查询」为隔离能力判据,删除会导致门禁 fail-closed 阻断部署。
     */
    default List<FloorPlanRecord> findAllFloorPlansByProject(String projectId) {
        return List.of();
    }

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
