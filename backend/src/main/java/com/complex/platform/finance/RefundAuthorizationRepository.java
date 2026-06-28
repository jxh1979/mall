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

    /** 配置：按 (subject_type, subject_id, refund_domain, project_id) 幂等 upsert 额度（B1d② 含 project 维度）。 */
    RefundAuthorityLimitRecord saveLimit(RefundAuthorityLimitRecord limit);

    /** B1d② 按综合体(project)隔离列额度：projectId 非空→该综合体 + 全局(NULL)；为空→全部（平台口径）。 */
    List<RefundAuthorityLimitRecord> listLimits(String projectId);

    /** 配置：按 (refund_domain, rule_type, project_id) 幂等 upsert 规则（B1d② 含 project 维度）。 */
    RefundAutoRuleRecord saveRule(RefundAutoRuleRecord rule);

    /** B1d② 按综合体(project)隔离列自动退规则：projectId 非空→该综合体 + 全局；为空→全部。 */
    List<RefundAutoRuleRecord> listRules(String projectId);
}
