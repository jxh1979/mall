package com.complex.platform.evidence.dto;

import java.util.List;

/**
 * 上报统一风险预警（report 动作，创建 open 状态预警）。
 */
public record RiskAlertReportRequest(
        String idempotencyKey,
        String sourceDomain,
        String sourceRef,
        String alertType,
        String riskLevel,
        String summary,
        String projectId,
        String reason,
        List<String> evidenceRefs
) {
}
