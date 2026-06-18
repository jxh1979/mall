package com.complex.platform.customer;

import java.time.Instant;

/**
 * 积分规则引擎配置（REQ-12, P0-6）。后台定义积分获取/有效期/过期/等级阈值。
 * level_threshold 为 JSON 文本（各等级累计积分阈值）。project_id 为综合体级隔离键。
 */
public record CustomerPointsRuleRecord(
        String id,
        String projectId,
        String ruleName,
        String earnMethod,
        long earnRatio,
        String levelThreshold,
        String validityType,
        Integer validityDays,
        String expireMode,
        String clearDay,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
