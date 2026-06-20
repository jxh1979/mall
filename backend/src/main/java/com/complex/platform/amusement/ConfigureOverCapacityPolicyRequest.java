package com.complex.platform.amusement;

import java.util.List;

/**
 * 配置游乐项目「超容处置策略」请求体（写入 V104 amusement_over_capacity_policy，按 project_id 幂等 upsert）。
 *
 * <p>operatingCapacity：运营容量（软上限，>=0）；核销在场达该值即「软超容」（告警放行）。
 * disposalMode：处置模式，必须为 {@code block_hard}（硬阻断+软告警）或 {@code warn_only}（仅告警留痕不阻断）。
 * enabled：是否启用该策略（省略默认 true）；停用后退化为「仅硬上限阻断、无软上限告警」（向后兼容）。
 */
public record ConfigureOverCapacityPolicyRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        Integer operatingCapacity,
        String disposalMode,
        Boolean enabled
) {
}
