package com.complex.platform.customer;

import java.time.Instant;

/** 连续签到阶梯奖励配置（streakDays→awardPoints）。 */
public record CustomerCheckInTierRecord(
        String id,
        String projectId,
        int streakDays,
        long awardPoints,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
