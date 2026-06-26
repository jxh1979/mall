package com.complex.platform.facility;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** 楼宇设施仓储（S1 设备台账；S2/S3 工单·巡检在各自切片扩展）。 */
public interface FacilityRepository {
    List<FacilityEquipmentRecord> listEquipment();

    Optional<FacilityEquipmentRecord> findEquipmentById(String id);

    void insertEquipment(FacilityEquipmentRecord record);

    void updateEquipment(FacilityEquipmentRecord record);

    /** 到期/超期待维保（next_maintenance_due <= asOf，排除停用/报废）。 */
    List<FacilityEquipmentRecord> listMaintenanceDue(LocalDate asOf);
}
