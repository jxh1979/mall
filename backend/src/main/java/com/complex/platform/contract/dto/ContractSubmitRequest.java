package com.complex.platform.contract.dto;

// 文件版本：V0.01

import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest.ParallelLine;
import java.util.List;

/**
 * 提交合同进统一审批（HP-C01，BSM L1349「发起时指定这条主线的最终审批人（默认总经理）……业务审核、财务审核、法务审核作为平行的
 * 相关部门线，各指定该部门的最终审批人」）：finalApproverId 可空=取「新建」类模板配置的默认最终审批人（两者皆空拒绝）；
 * parallelLines 须覆盖该模板要求的每条平行线（line_code → 该线最终审批人）；reason 为申请说明（留痕）。
 */
public record ContractSubmitRequest(
        String idempotencyKey,
        String finalApproverId,
        List<ParallelLine> parallelLines,
        String reason
) {
}
