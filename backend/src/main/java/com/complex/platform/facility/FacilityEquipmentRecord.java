package com.complex.platform.facility;

// 文件版本：V0.01

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
        Instant updatedAt,
        // V264 数据存档与审批后删除范式（照 V259 商户先例）。存档=软标记可恢复；删除=硬删走审批。
        Instant archivedAt,
        String archivedBy,
        String deleteStatus,
        String deleteRequestedBy,
        Instant deleteRequestedAt,
        String deleteReason,
        // 第十六批第 36 条（ENUM:267、BSM:1408「维保分定期保养、例行巡检、故障维修、安全检测、部件更换」）：最近一次维保完成登记的维保类型。
        String lastMaintenanceType) {

    /** 向后兼容构造器（V264 存档/删除前签名）：既有调用方零改动，默认未存档 + delete_status=none。 */
    public FacilityEquipmentRecord(
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
            Instant updatedAt) {
        this(id, name, equipmentType, spaceId, brand, model, vendor, responsibleUserId, status,
                maintenanceCycleDays, lastMaintenanceAt, nextMaintenanceDue, installDate, warrantyUntil,
                alarmSource, alarmNote, alarmRaisedAt, projectId, merchantId, createdAt, updatedAt,
                null, null, "none", null, null, null, null);
    }

    // --- V264 存档/删除范式：不可变 record 的字段更新辅助。 ---
    public FacilityEquipmentRecord withArchive(Instant archivedAt, String archivedBy, Instant updatedAt) {
        return new FacilityEquipmentRecord(id, name, equipmentType, spaceId, brand, model, vendor, responsibleUserId,
                status, maintenanceCycleDays, lastMaintenanceAt, nextMaintenanceDue, installDate, warrantyUntil,
                alarmSource, alarmNote, alarmRaisedAt, projectId, merchantId, createdAt, updatedAt,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason,
                lastMaintenanceType);
    }

    public FacilityEquipmentRecord withDelete(String deleteStatus, String deleteRequestedBy,
            Instant deleteRequestedAt, String deleteReason, Instant updatedAt) {
        return new FacilityEquipmentRecord(id, name, equipmentType, spaceId, brand, model, vendor, responsibleUserId,
                status, maintenanceCycleDays, lastMaintenanceAt, nextMaintenanceDue, installDate, warrantyUntil,
                alarmSource, alarmNote, alarmRaisedAt, projectId, merchantId, createdAt, updatedAt,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason,
                lastMaintenanceType);
    }

    public FacilityEquipmentRecord withLastMaintenanceType(String lastMaintenanceType) {
        return new FacilityEquipmentRecord(id, name, equipmentType, spaceId, brand, model, vendor, responsibleUserId,
                status, maintenanceCycleDays, lastMaintenanceAt, nextMaintenanceDue, installDate, warrantyUntil,
                alarmSource, alarmNote, alarmRaisedAt, projectId, merchantId, createdAt, updatedAt,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason,
                lastMaintenanceType);
    }
}
