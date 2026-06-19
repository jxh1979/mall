package com.complex.platform.customer;

import java.time.Instant;

/**
 * 会员等级配置（gap-analysis §7A P1：等级降级/保级宽限）。
 * keepThreshold=保级门槛；keepPeriodDays=考核周期；graceDays=宽限期天数；
 * pointMultiplierBasisPoints=该等级积分倍率（万分比，10000=1.0x）。
 */
public record CustomerMemberLevelRecord(
        String id,
        String projectId,
        String levelCode,
        String levelName,
        int levelRank,
        long upgradeThreshold,
        long keepThreshold,
        int keepPeriodDays,
        int graceDays,
        int pointMultiplierBasisPoints,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
