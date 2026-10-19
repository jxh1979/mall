package com.complex.platform.contract.dto;

// 文件版本：V0.01

import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest.ParallelLine;
import java.util.List;

/**
 * 退租/到期结算：按合同 settlement_rules 逐例算；带 disputeReason 则挂起「有争议」。
 * HP-C02（BSM L1352「退租、提前终止、合同到期等各种终止结算，都要经过多级审批」）：到期结算（triggerType=expiry）开账即按「到期终止」类模板开统一审批
 * 复核单——finalApproverId 可空=模板默认最终审批人，parallelLines 须覆盖模板要求的每条平行线，reason 为申请说明（留痕）；退租/提前终止的审批走退租变更，此三项不用。
 */
public record ContractSettleRequest(
        String idempotencyKey,
        String triggerType,
        String disputeReason,
        String finalApproverId,
        List<ParallelLine> parallelLines,
        String reason
) {
}
