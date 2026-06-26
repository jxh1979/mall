package com.complex.platform.contract;

/** 铺位到期着色分类（配置）：每类 = 分类名 + 颜色 + 提前天数。 */
public record SpaceExpiryColorRuleRecord(
        String id,
        String projectId,       // NULL=平台默认
        String categoryName,
        String color,
        int thresholdDays,
        int seq,
        boolean active
) {
}
