package com.complex.platform.amusement;

/**
 * §6.4 三维度容量扣减入参：核销时携带项目/场次/点位/设备维度键与项目安全上限。
 *
 * <p>locationPointId / deviceId 任一为空表示该范围不参与点位·设备维度匹配；
 * projectSafetyCapacity &lt;= 0 表示项目未设安全上限（该维度跳过，仅做汇总）。
 * peopleCount 为本次核销实际放行人数；多人票必须整体占用 N 个容量，任一维度不足则整体拒绝。
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
