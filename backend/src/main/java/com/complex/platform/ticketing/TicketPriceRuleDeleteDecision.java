package com.complex.platform.ticketing;

/** 价格规则删除审批结果。approved 时已物理删除（deleted=true）；rejected 回 none 未删除。 */
public record TicketPriceRuleDeleteDecision(String priceRuleId, String decision, boolean deleted) {
}
