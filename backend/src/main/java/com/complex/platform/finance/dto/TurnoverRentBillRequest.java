package com.complex.platform.finance.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * P1#14 营业额抽成计费：按抽成规则 × 回流营业额生成应收 ar_ap_items(source_type='turnover_rent')。
 * <p>计费引擎：rent = clamp(turnover × rate_bps / 10000, min_amount_cent, max_amount_cent)。
 * 回流营业额由服务层只读 integration_order_syncs 聚合该商户在 [periodStart, periodEnd] 的回流 order 金额。
 */
public record TurnoverRentBillRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String ruleId,
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDate dueDate
) {
}
