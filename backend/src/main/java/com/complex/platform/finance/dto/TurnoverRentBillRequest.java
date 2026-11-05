package com.complex.platform.finance.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * P1#14 营业额抽成计费：按抽成规则 × 本账期营业额生成应收 ar_ap_items(source_type='turnover_rent')。
 * <p>计费引擎：rent = clamp(turnover × rate_bps / 10000, min_amount_cent, max_amount_cent)。
 * 营业额由服务层聚合：integration_order_syncs 该商户在 [periodStart, periodEnd] 营业日窗内的回流金额（退款相抵）；
 * 酒吧商户另加本账期没收的订台定金（bar_reservations.deposit_settlement='forfeited'，以没收时点归期；第十八批 P 段第 131 条）
 * 与平台内酒吧收入（整单已确认商品实收＋月结服务费扣留−已退成商品分项，来源未定 409 BAR_REVENUE_PENDING；第十八批 Q 段第 138 条 138-R1）、
 * 已完成的服务点单售价全额（以下单时点归期；第十八批 Q 段第 138 条 138-R4），
 * 酒吧商户各来源按该酒吧营业日窗取数（第十八批 Q 段第 140 条）；非酒吧商户另加已付讫的券与通票合作方券结算金额
 * （partner_voucher 应付的实际付款，按付款时点归期；第十八批 Q 段第 138 条 138-R3）。
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
