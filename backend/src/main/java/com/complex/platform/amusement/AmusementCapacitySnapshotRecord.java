package com.complex.platform.amusement;

import java.time.Instant;

/**
 * 游乐 并发在场容量快照 只读项（阶段3 在场容量看板）。
 *
 * <p>两类容量维度共用一张 amusement_capacity_snapshot：
 * <ul>
 *   <li>{@code dimension=point_device}：自由进出型，occupied_count=当前在场，capacity_limit=并发上限。</li>
 *   <li>{@code dimension=project/session}：场次/批次型口径，occupied_count=已用，capacity_limit=容量上限。</li>
 * </ul>
 * 只读投影，不参与容量扣减/释放主逻辑。
 */
public record AmusementCapacitySnapshotRecord(
        String id,
        String projectId,
        String sessionId,
        String locationPointId,
        String deviceId,
        String dimension,
        int capacityLimit,
        int occupiedCount,
        Instant updatedAt
) {
}
