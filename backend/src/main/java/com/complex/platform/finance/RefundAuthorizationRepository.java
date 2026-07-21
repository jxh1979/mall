package com.complex.platform.finance;

import java.util.List;
import java.util.Optional;

/**
 * 退款分级授权主数据仓储（V156）：员工额度 + 自动退规则的读写。
 * 生产 {@code JdbcRefundAuthorizationRepository}，测试用内存桩。
 *
 * <p>AUD-008：运行时查询（{@link #findActiveLimit} / {@link #isAutoRuleEnabled}）必须带权威 {@code projectId}
 * （由原订单/账单/支付/操作者 scope 推导，不信任请求体）。决策顺序固定「当前综合体精确配置 → 全局(NULL)回退」，
 * 绝不跨到其它综合体：{@code projectId} 非空 → 只命中该综合体行或全局行，精确综合体优先；{@code projectId} 为空
 * → 只命中全局(NULL)行。
 */
public interface RefundAuthorizationRepository {

    /**
     * 查某主体在某域某综合体的 active 额度（精确综合体优先、全局(NULL)回退；域回退在 Service 层组合）。
     *
     * @param projectId 权威综合体 id；非空→该综合体行或全局行，精确综合体胜；为空→仅全局(NULL)行。
     */
    Optional<RefundAuthorityLimitRecord> findActiveLimit(
            String subjectType, String subjectId, String refundDomain, String projectId);

    /**
     * 某域某规则类型在某综合体是否启用（精确综合体优先、全局(NULL)回退）。
     *
     * @param projectId 权威综合体 id；非空→该综合体行或全局行，精确综合体胜；为空→仅全局(NULL)行。
     */
    boolean isAutoRuleEnabled(String refundDomain, String ruleType, String projectId);

    /** 配置：按 (subject_type, subject_id, refund_domain, project_id) 幂等 upsert 额度（B1d② 含 project 维度）。 */
    RefundAuthorityLimitRecord saveLimit(RefundAuthorityLimitRecord limit);

    /** B1d② 按综合体(project)隔离列额度：projectId 非空→该综合体 + 全局(NULL)；为空→全部（平台口径）。 */
    List<RefundAuthorityLimitRecord> listLimits(String projectId);

    /** 配置：按 (refund_domain, rule_type, project_id) 幂等 upsert 规则（B1d② 含 project 维度）。 */
    RefundAutoRuleRecord saveRule(RefundAutoRuleRecord rule);

    /** B1d② 按综合体(project)隔离列自动退规则：projectId 非空→该综合体 + 全局；为空→全部。 */
    List<RefundAutoRuleRecord> listRules(String projectId);
}
