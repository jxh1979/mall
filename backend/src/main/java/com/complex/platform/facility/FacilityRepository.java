package com.complex.platform.facility;

import java.util.List;
import java.util.Optional;

/** 楼宇设施设备台账（facility_equipment，V165）+ 维保工单（facility_work_orders，V166）仓储。 */
public interface FacilityRepository {

    FacilityEquipmentRecord saveEquipment(FacilityEquipmentRecord equipment);

    List<FacilityEquipmentRecord> listEquipment();

    Optional<FacilityEquipmentRecord> findEquipmentById(String id);

    FacilityWorkOrderRecord saveWorkOrder(FacilityWorkOrderRecord workOrder);

    List<FacilityWorkOrderRecord> listWorkOrders();

    Optional<FacilityWorkOrderRecord> findWorkOrderById(String id);
}
