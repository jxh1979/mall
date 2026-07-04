package com.complex.platform.analytics;

/** V221 商户级收入归因口径维护请求(经营参数页):新增/更新一条规则。 */
public record RevenueAttributionRuleRequest(
        String idempotencyKey,
        String id,
        String ruleScope,          // default / merchant
        String merchantId,         // rule_scope=merchant 时必填
        String revenueSource,      // ticket / bar / contract_rent / thirdparty
        String attributionMode,    // gross / share_bps / exclude
        Integer shareBps,          // share_bps 模式必填[0,10000]
        String refundAttribution,  // net_deduct / ignore
        String status,             // active / inactive
        Boolean isDraft
) {
}
