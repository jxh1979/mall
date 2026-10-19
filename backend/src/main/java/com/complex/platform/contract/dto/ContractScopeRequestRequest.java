package com.complex.platform.contract.dto;

// 文件版本：V0.01

import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest.ParallelLine;
import java.time.LocalDate;
import java.util.List;

/**
 * 超范围下单经营申请（#30，2026-07-19 拍板）：商户/现场下单被合同经营范围拦下后，可就某品类提交申请，
 * 按「变更」类合同模板开统一审批复核单（HP-C01），通过则授予该商户该品类临时/永久经营许可。
 *
 * <p>category = ContractScopeGuard.ALLOWED_CATEGORY_VALUES 之一（既有业务类目，禁发明枚举）；
 * grantType ∈ {permanent, temporary}，temporary 须带将来 validUntil；reason 必填留痕；
 * finalApproverId 可空=模板默认最终审批人，parallelLines 须覆盖模板要求的每条平行线。
 */
public record ContractScopeRequestRequest(
        String idempotencyKey,
        String category,
        String reason,
        String grantType,
        LocalDate validUntil,
        String finalApproverId,
        List<ParallelLine> parallelLines
) {
}
