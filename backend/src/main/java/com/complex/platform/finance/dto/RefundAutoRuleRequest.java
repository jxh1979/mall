package com.complex.platform.finance.dto;

/**
 * 退款自动放行规则配置（upsert，Stage C）。
 * 自然键 (refund_domain, rule_type) 幂等：已存在则更新 enabled / status。
 * status 缺省 active；enabled 缺省 false（最安全）。
 */
public record RefundAutoRuleRequest(
        String idempotencyKey,
        String refundDomain,       // ticket | bar | finance | all
        String ruleType,           // expired_unused | unused | ...
        Boolean enabled,           // true | false（缺省 false）
        String projectId,          // 可空
        String status              // active | inactive（缺省 active）
) {
}
