package com.complex.platform.inventory;

import java.time.Instant;

/**
 * R5 T17 五段交接链中的单段步骤。
 * actor_type=staff 为工作手机扫码主动留痕；actor_type=robot 为机器人上报被动留痕。
 * step_status：pending/done/gap/duplicate/diff。
 */
public record TraceHandoverStepRecord(
        String id,
        String handoverId,
        String segment,
        int seq,
        String actorType,
        String operatorUserId,
        String robotTaskRef,
        Instant scanAt,
        String stepStatus,
        String note,
        Instant createdAt
) {
}
