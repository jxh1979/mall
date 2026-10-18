package com.complex.platform.access.staff;

// 文件版本：V0.01

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;

/** 完整税表是审批原件；类别及适用身份来自专业认定，不由服务名称推断。 */
public record PayrollTaxVersion(String id,String category,String version,LocalDate effectiveFrom,
        String applicabilityBasis,String calculationBasis,String roundingRule,List<Bracket> brackets,
        String reason,String status,String reviewId,List<String> evidenceAttachmentIds,String createdBy,
        Instant createdAt,Instant decidedAt) {
    public record Bracket(Integer seq,Long incomeFloorCent,Long incomeCeilingCent,Integer incomeBaseBps,
            Long flatDeductionCent,Integer rateBps,Long quickDeductionCent) {}
    public record DraftRequest(String idempotencyKey,String category,LocalDate effectiveFrom,
            String applicabilityBasis,String calculationBasis,String roundingRule,List<Bracket> brackets,String reason) {}
    public record SubmitRequest(String idempotencyKey,List<String> evidenceAttachmentIds,
            List<SupervisorReviewEvaluateRequest.ParallelLine> parallelLines) {}
}
