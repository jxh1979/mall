package com.complex.platform.finance;

import java.util.List;
import java.util.Optional;

/**
 * 退款分级授权主数据仓储（V156）：员工额度 + 自动退规则的读写。
 * 生产 {@code JdbcRefundAuthorizationRepository}，测试用内存桩。
 */
public interface RefundAuthorizationRepository {

    /** 查某主体在某域的 active 额度（精确域命中，不做 'all' 回退——回退在 Service 层组合）。 */
    Optional<RefundAuthorityLimitRecord> findActiveLimit(String subjectType, String subjectId, String refundDomain);

    /** 某域某规则类型是否启用（active）。 */
    boolean isAutoRuleEnabled(String refundDomain, String ruleType);

    /** 配置：按 (subject_type, subject_id, refund_domain) 幂等 upsert 额度。 */
    RefundAuthorityLimitRecord saveLimit(RefundAuthorityLimitRecord limit);

    List<RefundAuthorityLimitRecord> listLimits();

    /** 配置：按 (refund_domain, rule_type) 幂等 upsert 规则。 */
    RefundAutoRuleRecord saveRule(RefundAutoRuleRecord rule);

    List<RefundAutoRuleRecord> listRules();
}
