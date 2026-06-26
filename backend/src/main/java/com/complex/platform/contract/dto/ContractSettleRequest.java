package com.complex.platform.contract.dto;

/** 退租/到期结算：按合同 settlement_rules 逐例算；带 disputeReason 则挂起「有争议」。 */
public record ContractSettleRequest(
        String idempotencyKey,
        String triggerType,
        String disputeReason
) {
}
