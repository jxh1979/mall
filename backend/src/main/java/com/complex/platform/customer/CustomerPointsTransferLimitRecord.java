package com.complex.platform.customer;

import java.time.Instant;

/**
 * 积分转赠风控配置（gap-analysis §7A P2：防套现）。
 * dailyPointsLimit=单客每日转出积分上限（0=不限）；dailyCountLimit=单客每日转出次数上限（0=不限）。
 */
public record CustomerPointsTransferLimitRecord(
        String id,
        String projectId,
        long dailyPointsLimit,
        int dailyCountLimit,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
