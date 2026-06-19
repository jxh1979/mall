package com.complex.platform.finance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 营业额抽成规则（P1#14）。rate = rateBps/10000；minAmountCent/maxAmountCent 为保底/封顶（可空）。
 */
public record TurnoverRentRuleRecord(
        String id,
        @JsonIgnore String merchantId,
        String ruleName,
        int rateBps,
        Long minAmountCent,
        Long maxAmountCent,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String status,
        Instant createdAt
) {
}
