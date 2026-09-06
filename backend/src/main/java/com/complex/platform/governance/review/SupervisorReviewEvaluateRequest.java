package com.complex.platform.governance.review;

import java.util.List;

/**
 * 手工评估入参。M087（PGM-1.7.1「发起单据时只需指定该事项的最终审批人」「需要平行线审批的另行指定该条线的最终审批人」）：
 * finalApproverId 为主线最终审批人（可空：按策略档位下限沿直接上级链生成）；parallelLines 为平行部门线。
 */
public record SupervisorReviewEvaluateRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String actionType,
        String objectType,
        String objectId,
        Long amountCent,
        String violationSummary,
        String finalApproverId,
        List<ParallelLine> parallelLines
) {
    /** 平行线：line_code 对应 approval_lines 设置；finalApproverId 为该线最终审批人。 */
    public record ParallelLine(String lineCode, String finalApproverId) {
    }
}
