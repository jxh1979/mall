package com.complex.platform.evidence.dto;

import java.util.List;

/**
 * 责任链动作通用请求（claim/transfer/escalate/dismiss）。
 *
 * <ul>
 *   <li>transfer：{@code targetHandlerId} 必填（转交给谁）。</li>
 *   <li>escalate：{@code targetHandlerId} 必填（升级给谁）。</li>
 *   <li>claim/dismiss：{@code targetHandlerId} 忽略。</li>
 * </ul>
 *
 * resolve 动作使用独立的 {@link RiskAlertResolveRequest}（强制 resolutionConclusion）。
 */
public record RiskAlertActionRequest(
        String idempotencyKey,
        String targetHandlerId,
        String reason,
        List<String> evidenceRefs
) {
}
