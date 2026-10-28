package com.complex.platform.leasing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * 招商意向（leasing_intents，V161 重建）：5 阶段独立流转（非旧版报价状态机）。
 * stage 对齐业务 HTML §10：initial_contact/requirement_confirmed/proposal_quoted/negotiation/pending_signing。
 * 关键阶段（proposal_quoted/negotiation）推进需主管确认（第十八批第 30 条，PGM L502）：送审时记 pendingStage／pendingReviewId，
 * 批准才进入并由 lastStageChangeBy 记确认人，批准或驳回都清空待确认（V563）。
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
        Instant updatedAt,
        String pendingStage,
        String pendingReviewId
) {
    /** 无待确认阶段的构造（新建、非关键阶段推进与测试夹具）。 */
    public LeasingIntentRecord(String id, String leadId, String stage, String intendedSpaceId, BigDecimal intendedAreaSqm,
            Map<String, Object> rentTerms, Instant lastStageChangeAt, String lastStageChangeBy, String projectId,
            Instant createdAt, Instant updatedAt) {
        this(id, leadId, stage, intendedSpaceId, intendedAreaSqm, rentTerms, lastStageChangeAt, lastStageChangeBy, projectId,
                createdAt, updatedAt, null, null);
    }
}
