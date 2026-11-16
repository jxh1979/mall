package com.complex.platform.finance.dto;

// 文件版本：V0.01

import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import java.util.List;

/**
 * 第十八批 AM 段第 241 条 241-R1（BSM:1803「发起时指定这条主线的最终审批人……财务、法务及相关部门作为平行的相关部门线，各指定该部门的最终审批人」；
 * 第十八批裁决 68「同收款方、同类型、同综合体可勾选合发」）：台账勾选应付合并发起付款确认。parallel_lines 须含财务、法务两线各自的最终审批人
 * （裁决 66），可另带在用的线。
 */
public record ArApPaymentConfirmationRequest(
        String idempotencyKey,
        List<String> arApItemIds,
        String reason,
        String finalApproverId,
        List<SupervisorReviewEvaluateRequest.ParallelLine> parallelLines
) {
}
