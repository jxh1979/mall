package com.complex.platform.finance;

import java.time.Instant;

/**
 * 退款自动规则（refund_auto_rules，V156）：某退款域的某规则类型（如到期未用 expired_unused）是否启用。
 * 启用后，命中该规则的退款由系统按规则实时自动退，零人工。
 */
public record RefundAutoRuleRecord(
        String id,
        String refundDomain,
        String ruleType,
        boolean enabled,
        String projectId,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
