package com.complex.platform.amusement;

import java.time.Instant;

/**
 * 游乐设备主数据记录（amusement_devices，V143）。
 * 承载设备编码（桥接旧 device_id 字符串）、状态、每设备安全检查周期、最近 PASS 巡检时间。
 * safetyCheckIntervalHours 为空表示回退项目级缺省周期（amusement_projects.safety_check_interval_hours）。
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
        Instant updatedAt
) {
}
