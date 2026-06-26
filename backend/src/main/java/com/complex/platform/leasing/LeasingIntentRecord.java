package com.complex.platform.leasing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * 招商意向（leasing_intents，V161 重建）：5 阶段独立流转（非旧版报价状态机）。
 * stage 对齐业务 HTML §10：initial_contact/requirement_confirmed/proposal_quoted/negotiation/pending_signing。
 * 关键阶段（proposal_quoted/negotiation）推进需主管确认（lastStageChangeBy 记确认人）。
 */
public record LeasingIntentRecord(
        String id,
        String leadId,
        String stage,
        String intendedSpaceId,
        BigDecimal intendedAreaSqm,
        Map<String, Object> rentTerms,
        Instant lastStageChangeAt,
        String lastStageChangeBy,
        String projectId,
        Instant createdAt,
        Instant updatedAt
) {
}
