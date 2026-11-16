package com.complex.platform.finance.dto;

// 文件版本：V0.01

import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import java.util.List;

/**
 * 第十八批 AM 段第 241 条 241-R4、241-R5（BSM:1803「发起时指定这条主线的最终审批人……财务、法务及相关部门作为平行的相关部门线，各指定该部门的
 * 最终审批人」；第十八批裁决 55「退款、工资、货款各类都要设流程」、68「工资、退款、赔付按单发起」）：单个付款对象（工资应付、财务退款待办、门票退款
 * 打款）发起付款确认。parallel_lines 须含财务、法务两线各自的最终审批人（裁决 66），可另带在用的线。
 */
public record PaymentConfirmationRequest(
        String idempotencyKey,
        String reason,
        String finalApproverId,
        List<SupervisorReviewEvaluateRequest.ParallelLine> parallelLines
) {
}
