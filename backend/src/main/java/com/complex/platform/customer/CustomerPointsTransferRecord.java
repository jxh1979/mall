package com.complex.platform.customer;

import java.time.Instant;

/**
 * 积分转赠流水（gap-analysis §7A P2：会员间转移）。
 * from 扣 / to 加 原子事务；status=completed|rejected|reversed；idempotencyKey 唯一防重复。
 */
public record CustomerPointsTransferRecord(
        String id,
        String fromCustomerId,
        String toCustomerId,
        long points,
        String status,
        String reason,
        String idempotencyKey,
        Instant createdAt
) {
}
