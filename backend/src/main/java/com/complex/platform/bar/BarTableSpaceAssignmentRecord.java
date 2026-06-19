package com.complex.platform.bar;

import java.time.Instant;

/**
 * 卡座/桌台空间指派（回填）结果。
 *
 * <p>仅回读 table_id + 已设置的 space_id（不改动 {@link BarTableRecord} 结构），便于运营核对回填。</p>
 */
public record BarTableSpaceAssignmentRecord(
        String tableId,
        String spaceId,
        Instant assignedAt
) {
}
