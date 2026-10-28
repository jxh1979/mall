package com.complex.platform.customer;

import java.time.Instant;

/**
 * 生日加倍规则（gap-analysis §7A P2：生日加倍）。
 * windowType=birthday_day（仅生日当天）/birthday_month（生日当月）；
 * multiplierBasisPoints=倍率万分比（>10000，如 20000=2.0x）。
 * 第十八批第 12 条同根：生日加倍规则全平台一套（BSM L831「生日倍率与生日窗口全国平台统一规定，不按项目分别配置」）。
 */
public record CustomerBirthdayRuleRecord(
        String id,
        String ruleName,
        String windowType,
        int multiplierBasisPoints,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
