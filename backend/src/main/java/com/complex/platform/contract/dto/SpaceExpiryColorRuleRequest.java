package com.complex.platform.contract.dto;

/** 配置到期着色分类（分类名 + 颜色 + 提前天数）；按综合体，带 id 即改本综合体的这一条，active=false 即移除（386-R1、386-R3）。 */
public record SpaceExpiryColorRuleRequest(
        String idempotencyKey,
        String projectId,
        String categoryName,
        String color,
        Integer thresholdDays,
        Integer seq,
        String id,
        Boolean active
) {
}
