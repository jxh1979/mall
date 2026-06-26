package com.complex.platform.leasing;

import java.time.Instant;

/**
 * 招商跟进记录（leasing_follow_ups，V161 重建）：线索/意向通用流水。
 * refType：lead/intent。3 天未跟进提醒由应用层按线索 lastFollowUpAt 计算。
 */
public record LeasingFollowUpRecord(
        String id,
        String refType,
        String refId,
        String method,
        String content,
        Instant nextPlanAt,
        String operatorUserId,
        String projectId,
        Instant createdAt
) {
}
