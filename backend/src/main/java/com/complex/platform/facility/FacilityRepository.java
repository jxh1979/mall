package com.complex.platform.facility;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** 楼宇设施仓储（S1 设备台账；S2/S3 工单·巡检在各自切片扩展）。 */
public interface FacilityRepository {
    List<FacilityEquipmentRecord> listEquipment();

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
}
