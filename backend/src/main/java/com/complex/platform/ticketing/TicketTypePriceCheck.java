package com.complex.platform.ticketing;

import java.math.BigDecimal;

/**
 * V369 票种定价核算（业务 HTML:741「综合／联合通票的定价构成」）。
 *
 * <p>给运营看的一张对照表：这张票各业态票面原价加起来多少、打几折、系统算出来该卖多少、
 * 现在填的是多少、差了多少、后台允许差多少、超没超。建票/改票时前端拿它做实时提示，
 * 发布闸拒绝时报的也是同一组数——运营看到的和系统判的是同一份账。
 *
 * @param componentTotalCent Σ(各权益票面原价 × 份数)，免费权益（0）自然不计入
 * @param comboDiscountRate  票种折扣（百分数，100=不打折）
 * @param computedPriceCent  系统算出售价 = componentTotalCent × 折扣 ÷ 100（四舍五入到分）
 * @param filledPriceCent    运营现在填的售价（平日价）；未填为 null
 * @param diffCent           填的 − 算出的（带符号：正=上调、负=下调）；未填价为 null
 * @param toleranceCent      后台可配的允许误差（分）
 * @param withinTolerance    是否在允许误差内（未填价视为不通过）
 * @param roundingSuggested  按「整元且尾数 8/9/0」建议的凑整售价（分），供前端提示运营填哪个数好卖
 */
public record TicketTypePriceCheck(
        String ticketTypeId,
        long componentTotalCent,
        BigDecimal comboDiscountRate,
        long computedPriceCent,
        Long filledPriceCent,
        Long diffCent,
        long toleranceCent,
        boolean withinTolerance,
        long roundingSuggested
) {
}
