package com.complex.platform.evidence;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

/**
 * 统一风险预警主记录（跨域汇聚层，evidence-risk-layer-spec §8）。
 *
 * <p>状态机：open → handling → escalated → resolved / dismissed。
 * resolved 必须带 {@code resolutionConclusion}（服务层 + DB CHECK 双校验）。
 */
public record RiskAlertRecord(
        String alertId,
        String sourceDomain,
        String sourceRef,
        String alertType,
        String riskLevel,
        String status,
        String summary,
        String firstReportedBy,
        String firstHandlerId,
        String escalatedTo,
        String resolvedBy,
        String resolutionConclusion,
        String projectId,
        @JsonIgnore String merchantId,
        @JsonIgnore String idempotencyKey,
        Instant createdAt,
        Instant updatedAt
) {

    public RiskAlertRecord withStatus(String newStatus) {
        return new RiskAlertRecord(alertId, sourceDomain, sourceRef, alertType, riskLevel, newStatus, summary,
                firstReportedBy, firstHandlerId, escalatedTo, resolvedBy, resolutionConclusion,
                projectId, merchantId, idempotencyKey, createdAt, updatedAt);
    }

    public RiskAlertRecord withFirstHandlerId(String newFirstHandlerId) {
        return new RiskAlertRecord(alertId, sourceDomain, sourceRef, alertType, riskLevel, status, summary,
                firstReportedBy, newFirstHandlerId, escalatedTo, resolvedBy, resolutionConclusion,
                projectId, merchantId, idempotencyKey, createdAt, updatedAt);
    }

    public RiskAlertRecord withEscalatedTo(String newEscalatedTo) {
        return new RiskAlertRecord(alertId, sourceDomain, sourceRef, alertType, riskLevel, status, summary,
                firstReportedBy, firstHandlerId, newEscalatedTo, resolvedBy, resolutionConclusion,
                projectId, merchantId, idempotencyKey, createdAt, updatedAt);
    }

    public RiskAlertRecord withResolvedBy(String newResolvedBy) {
        return new RiskAlertRecord(alertId, sourceDomain, sourceRef, alertType, riskLevel, status, summary,
                firstReportedBy, firstHandlerId, escalatedTo, newResolvedBy, resolutionConclusion,
                projectId, merchantId, idempotencyKey, createdAt, updatedAt);
    }

    public RiskAlertRecord withResolutionConclusion(String newConclusion) {
        return new RiskAlertRecord(alertId, sourceDomain, sourceRef, alertType, riskLevel, status, summary,
                firstReportedBy, firstHandlerId, escalatedTo, resolvedBy, newConclusion,
                projectId, merchantId, idempotencyKey, createdAt, updatedAt);
    }

    public RiskAlertRecord touched(Instant updatedAtNow) {
        return new RiskAlertRecord(alertId, sourceDomain, sourceRef, alertType, riskLevel, status, summary,
                firstReportedBy, firstHandlerId, escalatedTo, resolvedBy, resolutionConclusion,
                projectId, merchantId, idempotencyKey, createdAt, updatedAtNow);
    }
}
