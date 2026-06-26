package com.complex.platform.facility;

import java.time.Instant;
import java.time.LocalDate;

/** 楼宇机电设备台账记录（V165）。状态6态、4分类、每台维保周期、告警源。 */
public record FacilityEquipmentRecord(
        String id,
        String name,
        String equipmentType,
        String spaceId,
        String brand,
        String model,
        String vendor,
        String responsibleUserId,
        String status,
        Integer maintenanceCycleDays,
        Instant lastMaintenanceAt,
        LocalDate nextMaintenanceDue,
        LocalDate installDate,
        LocalDate warrantyUntil,
        String alarmSource,
        String alarmNote,
        Instant alarmRaisedAt,
        String projectId,
        String merchantId,
        Instant createdAt,
        Instant updatedAt) {}
