package com.complex.platform.facility;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** 楼宇设施设备台账（V165）+ 维保工单（V166）+ 巡检模板/任务/记录（V167）仓储。 */
public interface FacilityRepository {

    FacilityEquipmentRecord saveEquipment(FacilityEquipmentRecord equipment);

    List<FacilityEquipmentRecord> listEquipment();

    Optional<FacilityEquipmentRecord> findEquipmentById(String id);

    FacilityWorkOrderRecord saveWorkOrder(FacilityWorkOrderRecord workOrder);

    List<FacilityWorkOrderRecord> listWorkOrders();

    Optional<FacilityWorkOrderRecord> findWorkOrderById(String id);

    FacilityInspectionTemplateRecord saveInspectionTemplate(FacilityInspectionTemplateRecord template);

    List<FacilityInspectionTemplateRecord> listInspectionTemplates();

    /** 按自然键 (equipmentId, dueAt) 幂等插入；返回是否真正新增（冲突即 false）。 */
    boolean upsertInspectionTask(FacilityInspectionTaskRecord task);

    /** 按 id upsert（完成时更新 status/completed_*）。 */
    FacilityInspectionTaskRecord saveInspectionTask(FacilityInspectionTaskRecord task);

    List<FacilityInspectionTaskRecord> listInspectionTasks();

    Optional<FacilityInspectionTaskRecord> findInspectionTaskById(String id);

    /** pending 且 due_at<now → overdue；返回翻转条数。 */
    int markOverdueInspectionTasks(Instant now);

    FacilityInspectionRecordRecord saveInspectionRecord(FacilityInspectionRecordRecord record);
}
