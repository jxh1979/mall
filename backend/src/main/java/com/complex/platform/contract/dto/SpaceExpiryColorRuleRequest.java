package com.complex.platform.contract.dto;

/** 配置到期着色分类（分类名 + 颜色 + 提前天数）。 */
public record SpaceExpiryColorRuleRequest(
        String idempotencyKey,
        String projectId,
        String categoryName,
        String color,
        Integer thresholdDays,
        Integer seq
) {
}
