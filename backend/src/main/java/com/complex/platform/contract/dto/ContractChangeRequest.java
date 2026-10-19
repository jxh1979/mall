package com.complex.platform.contract.dto;

// 文件版本：V0.01

import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest.ParallelLine;
import java.util.List;
import java.util.Map;

/**
 * 发起合同变更（6类，删转租）。HP-C01：变更按「变更」类模板开统一审批复核单——finalApproverId 可空=模板默认最终审批人，
 * parallelLines 须覆盖模板要求的每条平行线，reason 为变更说明（留痕）。
 */
public record ContractChangeRequest(
        String idempotencyKey,
        String changeType,
        Map<String, Object> payload,
        String finalApproverId,
        List<ParallelLine> parallelLines,
        String reason
) {
}
