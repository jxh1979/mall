package com.complex.platform.facility.dto;

import java.time.LocalDate;

/** 楼宇设备建档请求（③ S1）。equipmentType 必填且在枚举内；维保周期可空。 */
public record FacilityEquipmentRequest(
        String name,
        String equipmentType,
        String spaceId,
        String vendor,
        String model,
        LocalDate installDate,
        LocalDate warrantyUntil,
        Integer maintenanceCycleDays,
        String idempotencyKey
) {
}
