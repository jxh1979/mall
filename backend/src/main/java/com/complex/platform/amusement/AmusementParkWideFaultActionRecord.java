package com.complex.platform.amusement;

import java.time.Instant;

/** CG-08a 全园故障状态留痕：发起 / 确认生效 / 发起解除 / 确认解除各一行。 */
public record AmusementParkWideFaultActionRecord(
        String id,
        String faultId,
        String projectId,
        String action,
        String actorId,
        String actorRole,
        String note,
        Instant createdAt
) {
}
