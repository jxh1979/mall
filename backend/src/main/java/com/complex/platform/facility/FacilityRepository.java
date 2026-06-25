package com.complex.platform.facility;

import java.util.List;
import java.util.Optional;

/** 楼宇设施设备台账仓储（facility_equipment，V165）。 */
public interface FacilityRepository {

    FacilityEquipmentRecord saveEquipment(FacilityEquipmentRecord equipment);

    List<FacilityEquipmentRecord> listEquipment();

    Optional<FacilityEquipmentRecord> findEquipmentById(String id);
}
