package com.complex.platform.finance.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 建提成租金规则请求（填合同条款时）：挂合同 contractId + 阶梯超额累进 tiers（按 upperBoundCent 升序、末档 upper 为空=无上限）。
 * 保底 minAmountCent 可空=无保底。无封顶（不接受 max）。
 */
public record TurnoverRentRuleRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String contractId,
        String merchantId,
        String ruleName,
        Long minAmountCent,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        List<TierInput> tiers
) {
    public record TierInput(Long upperBoundCent, Integer rateBps) {
    }
}
