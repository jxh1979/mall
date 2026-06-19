package com.complex.platform.compliance;

import java.time.Instant;

/**
 * P0#8 企微违规命中入复核队列的写入口径（孤岛 analytics_risk_alerts，V017）。
 *
 * <p>统一异常表 risk_alerts 由别的代理建，此刻不存在；本期命中入复核写既有 analytics_risk_alerts，
 * 并统一标 {@code sourceDomain='wecom'}（V106 新增可空列），便于统一表就位后按 source_domain=wecom 迁移/聚合。
 * {@code idempotencyKey} 保证同一命中（hit_id）重复同步不产生重复告警。
 */
public record WecomRiskAlertEnqueue(
        String alertId,
        String ruleId,
        String riskLevel,
        String objectType,
        String objectId,
        String summary,
        String idempotencyKey,
        Instant createdAt
) {
}
