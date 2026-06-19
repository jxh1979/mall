package com.complex.platform.compliance;

import java.time.Instant;

/**
 * P0#8 企微违规命中记录（wecom_risk_hits，V106）。
 *
 * <p>一条命中 = 一条消息（{@code messageId}）× 一个企微违规规则码（{@code ruleCode}）。
 * {@code matchedSnippet} 为脱敏后命中片段（普通展示用，不含明文 PII）。
 * status 流转：open（待复核）→ reviewing（已领取）→ confirmed（确认违规）/ dismissed（误报驳回）。
 * {@code alertId} 关联复核队列条目（analytics_risk_alerts.id，source_domain=wecom；统一表就位后可改指向）。
 */
public record WecomRiskHitRecord(
        String hitId,
        String conversationId,
        String messageId,
        String ruleCode,
        String riskLevel,
        String matchedSnippet,
        String status,
        String reviewerId,
        String reviewNote,
        String projectId,
        String alertId,
        Instant createdAt,
        Instant updatedAt
) {
}
