package com.complex.platform.evidence.dto;

import java.util.List;

/**
 * 关闭（resolve）统一风险预警。{@code resolutionConclusion} 必填——
 * "异常关闭必须有结论"由服务层 + DB CHECK 双校验。
 */
public record RiskAlertResolveRequest(
        String idempotencyKey,
        String resolutionConclusion,
        String reason,
        List<String> evidenceRefs
) {
}
