package com.complex.platform.amusement;

// 文件版本：V0.01

/**
 * 核销上下文保留场次、点位及设备用于关联事实；硬容量只按项目实时在玩人数。
 * projectSafetyCapacity 必须为正，peopleCount 为本次整体入场人数。
 */
public record CapacityClaimContext(
        String projectId,
        String sessionId,
        String locationPointId,
        String deviceId,
        int projectSafetyCapacity,
        int peopleCount
) {
}
