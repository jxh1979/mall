package com.complex.platform.access.staff;

// 文件版本：V0.01

import java.time.Instant;

/** U06-004：临时顶班记录——只追加、不改写原班次、不走排班审批（BSM L1443）。 */
public record StaffShiftCoverRecord(
        String id,
        String shiftId,
        String originalUserId,
        String coverUserId,
        String arrangedBy,
        String reason,
        Instant createdAt
) {
}
