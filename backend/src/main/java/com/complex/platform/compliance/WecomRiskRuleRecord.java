package com.complex.platform.compliance;

import java.util.List;

/**
 * P0#8 企微违规规则码（读自 analytics_risk_rules，domain=compliance、enabled=true）。
 *
 * <p>由硬编码 RISK_KEYWORDS 升级为可配置规则数据：{@code keywords} 取自 rule_config_json.keywords，
 * {@link WecomConversationService#detectRiskHits} 按启用规则的关键词命中产出 {@link WecomRiskHitRecord}。
 * 规则启停（enabled）与等级（riskLevel）以库为准，关键词后台可增减。
 */
public record WecomRiskRuleRecord(
        String ruleId,
        String ruleCode,
        String riskLevel,
        List<String> keywords
) {
}
