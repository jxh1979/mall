package com.complex.platform.amusement;

import java.time.Instant;

/** 游乐项目点位/设备并发容量配置记录（amusement_capacity_config）。 */
public record AmusementCapacityConfigRecord(
        String id,
        String projectId,
        String locationPointId,
        String deviceId,
        int maxConcurrent,
        String effectiveSessionPattern,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
