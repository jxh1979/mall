package com.complex.platform.contract.dto;

import java.time.LocalDate;

/**
 * 超范围下单经营申请（#30，2026-07-19 拍板）：商户/现场下单被合同经营范围拦下后，可就某品类提交申请，
 * 走既有合同审批链（contract.approve.* 分组并行会签），通过则授予该商户该品类临时/永久经营许可。
 *
 * <p>category = ContractScopeGuard.ALLOWED_CATEGORY_VALUES 之一（既有业务类目，禁发明枚举）；
 * grantType ∈ {permanent, temporary}，temporary 须带将来 validUntil；reason 必填留痕。
 */
public record ContractScopeRequestRequest(
        String idempotencyKey,
        String category,
        String reason,
        String grantType,
        LocalDate validUntil
) {
}
