package com.complex.platform.facility;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 楼宇机电设备台账（facility_equipment，V165，③ 楼宇运营 S1）：楼宇侧设施设备主数据（HVAC/电梯/消防/给排水/配电/弱电）。
 * 工单(S2)/巡检(S3)/能耗(S4) 的引用地基。status: active|maintenance|retired。
 */
public record FacilityEquipmentRecord(
        String id,
        String name,
        String equipmentType,
        String spaceId,
        String vendor,
        String model,
        LocalDate installDate,
        LocalDate warrantyUntil,
        String status,
        Integer maintenanceCycleDays,
        Instant lastMaintenanceAt,
        String projectId,
        String merchantId,
        Instant createdAt,
        Instant updatedAt
) {
}
