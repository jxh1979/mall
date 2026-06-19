package com.complex.platform.amusement;

import java.time.Instant;

/**
 * 游乐项目空间指派（回填）结果。
 *
 * <p>仅回读 project_id + 已设置的 space_id（不改动 {@link AmusementProjectRecord} 结构），便于运营核对回填。</p>
 */
public record AmusementProjectSpaceAssignmentRecord(
        String projectId,
        String spaceId,
        Instant assignedAt
) {
}
