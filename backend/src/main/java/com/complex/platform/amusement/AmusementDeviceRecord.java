package com.complex.platform.amusement;

import java.time.Instant;

/**
 * 游乐设备主数据记录（amusement_devices，V143）。
 * 承载设备编码（桥接旧 device_id 字符串）、状态、每设备安全检查周期、最近 PASS 巡检时间。
 * safetyCheckIntervalHours 为空表示回退项目级缺省周期（amusement_projects.safety_check_interval_hours）。
 * 第十八批 CF 段第 407 条 407-R1：safetyClass（critical 关键安全设备、auxiliary 辅助设备）与 dependency（关键设备的 dependent 依赖、independent 不依赖）
 * 为工程或安全复核通过后生效的分级，两者同时为空表示分级还没生效。
 */
public record AmusementDeviceRecord(
        String id,
        String projectId,
        String deviceCode,
        String name,
        String status,
        Integer safetyCheckIntervalHours,
        String locationPointId,
        Instant lastPassInspectionAt,
        String merchantId,
        Instant createdAt,
        Instant updatedAt,
        String safetyClass,
        String dependency
) {

    /** 分级未生效（两列为空）的设备记录。 */
    public AmusementDeviceRecord(String id, String projectId, String deviceCode, String name, String status,
            Integer safetyCheckIntervalHours, String locationPointId, Instant lastPassInspectionAt, String merchantId,
            Instant createdAt, Instant updatedAt) {
        this(id, projectId, deviceCode, name, status, safetyCheckIntervalHours, locationPointId, lastPassInspectionAt, merchantId,
                createdAt, updatedAt, null, null);
    }
}
