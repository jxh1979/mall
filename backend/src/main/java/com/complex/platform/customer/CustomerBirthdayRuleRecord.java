package com.complex.platform.customer;

import java.time.Instant;

/**
 * 生日加倍规则（gap-analysis §7A P2：生日加倍）。
 * windowType=birthday_day（仅生日当天）/birthday_month（生日当月）；
 * multiplierBasisPoints=倍率万分比（>10000，如 20000=2.0x）。
 */
public record CustomerBirthdayRuleRecord(
        String id,
        String projectId,
        String ruleName,
        String windowType,
        int multiplierBasisPoints,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
