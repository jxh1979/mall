package com.complex.platform.leasing.dto;

import java.time.Instant;

/** 跟进记录创建请求（V161 重建）。refType：lead/intent；创建后回写线索 lastFollowUpAt（3天提醒依据）。 */
public record LeasingFollowUpRequest(
        String refType,
        String refId,
        String method,
        String content,
        Instant nextPlanAt,
        String idempotencyKey
) {
}
