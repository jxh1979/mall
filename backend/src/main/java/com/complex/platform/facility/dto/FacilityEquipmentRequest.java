package com.complex.platform.facility.dto;

import java.time.LocalDate;

/** 新建/更新设备台账请求。 */
public record FacilityEquipmentRequest(
        String id,
        String name,
        String equipmentType,
        String spaceId,
        String brand,
        String model,
        String vendor,
        String responsibleUserId,
        Integer maintenanceCycleDays,
        LocalDate installDate,
        LocalDate warrantyUntil,
        String projectId,
        String merchantId,
        String idempotencyKey) {}
