package com.complex.platform.finance;

// 文件版本：V0.01

import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import java.time.Instant;
import java.util.List;

/**
 * 经营主体税费实际申报金额登记（第十八批 AC 段第 210 条 210-R2，第十八批裁决 42；V616 第 2 节 entity_tax_declarations）。
 * status：pending_review 待财税法审核与最高确认 → approved 已生效／rejected 已驳回（可重登）。
 */
public record EntityTaxDeclaration(
        String id,
        String businessEntityId,
        String entityName,
        String taxType,
        String periodStartMonth,
        String periodEndMonth,
        long declaredAmountCent,
        String reason,
        List<String> evidenceAttachmentIds,
        String status,
        String reviewId,
        String createdBy,
        Instant createdAt,
        Instant approvedAt
) {
    /** 登记即提交审批：parallel_lines 为财务、税务、法务三条线各自的最终审批人（照 PayrollTaxVersion.SubmitRequest）。 */
    public record Request(
            String idempotencyKey,
            String businessEntityId,
            String taxType,
            String periodStartMonth,
            String periodEndMonth,
            Long declaredAmountCent,
            String reason,
            List<String> evidenceAttachmentIds,
            List<SupervisorReviewEvaluateRequest.ParallelLine> parallelLines
    ) {
    }
}
