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
 * （partner_voucher 应付的实际付款，按付款时点归期；第十八批 Q 段第 138 条 138-R3），另计通票分账专属直付应付（ticket_distribution）
 * 与补偿履约应付（compensation_fulfillment）的实际付款，两类追收应收的实际收回相抵（第十八批 X 段第 175 条 175-R1）。
 * 第十八批 U 段第 155 条 155-R1：出账同一事务逐笔快照酒吧整单与餐饮、零售回流订单的原账期档位费率分段；所退账单在之前账期结清的商品退款不进本期基数，
 * 原月按保底收租的不冲回，否则按原档位费率原额逐笔挂营业额抽成冲正应付（turnover_rent_reversal）；原账期抽成未出账 409 TURNOVER_RENT_ORIGIN_UNBILLED。
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
