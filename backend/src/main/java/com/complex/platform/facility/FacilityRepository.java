package com.complex.platform.facility;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** 楼宇设施仓储（S1 设备台账；S2/S3 工单·巡检在各自切片扩展）。 */
public interface FacilityRepository {
    List<FacilityEquipmentRecord> listEquipment();

    /** V264：includeArchived=false（默认）排除已存档/已审批删除的设备。默认委托旧签名（内存桩无存档概念）。 */
    default List<FacilityEquipmentRecord> listEquipment(boolean includeArchived) {
        return listEquipment();
    }

    default List<FacilityEquipmentRecord> listEquipmentByProject(String projectId) {
        return listEquipment().stream()
                .filter(record -> Objects.equals(projectId, record.projectId()))
                .toList();
    }

    Optional<FacilityEquipmentRecord> findEquipmentById(String id);

    void insertEquipment(FacilityEquipmentRecord record);

    void updateEquipment(FacilityEquipmentRecord record);

    /** 到期/超期待维保（next_maintenance_due <= asOf，排除停用/报废）。 */
    List<FacilityEquipmentRecord> listMaintenanceDue(LocalDate asOf);

    default List<FacilityEquipmentRecord> listMaintenanceDueByProject(LocalDate asOf, String projectId) {
        return listMaintenanceDue(asOf).stream()
                .filter(record -> Objects.equals(projectId, record.projectId()))
                .toList();
    }

    /** V264：设备活引用计数（&gt;0 拒删）；关联工单 facility_work_orders.equipment_id 为活引用（不级联）。内存桩默认 0。 */
    default long countEquipmentReferences(String id) {
        return 0L;
    }

    /** V264：物理删除设备。内存桩默认无操作。 */
    default void deleteEquipment(String id) {
    }

    /** V271 存档理由定向写入(reason 非空=存档,null=取消存档清空)。 */
    void setEquipmentArchiveReason(String id, String reason);
}
