package com.complex.platform.leasing;

import java.time.Instant;

/**
 * 招商品牌/客户库（leasing_brands，V161 重建）：4 类对齐业务 HTML §10。
 * brandType：intentional(意向)/prospective(储备)/partner(合作)/blacklist(黑名单)。
 * 黑名单品牌拦截同品牌新线索；同综合体内品牌名唯一（线索去重依据）。
 */
public record LeasingBrandRecord(
        String id,
        String brandName,
        String brandType,
        String businessType,
        String blacklistReason,
        String blacklistedBy,
        Instant blacklistedAt,
        String projectId,
        Instant createdAt,
        Instant updatedAt
) {
}
