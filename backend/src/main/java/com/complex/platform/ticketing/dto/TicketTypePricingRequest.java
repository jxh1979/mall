package com.complex.platform.ticketing.dto;

import java.math.BigDecimal;

/**
 * V369 改票种售价／折扣请求（业务 HTML:741「运营可在此基础上填一个近似售价用于凑整……上调下调均可」）。
 *
 * <p>运营配完各业态权益的票面原价后，系统会算出这张票该卖多少；本请求就是把「折扣」和「凑整后的售价」填回去。
 * 保存当场比对：与算出售价的差额超过后台可配的允许误差即拒绝（不等到发布才报错）。
 *
 * @param comboDiscountRate 票种折扣（百分数 0~100，100=不打折）；留空=不打折
 * @param weekdayPriceCent  平日售价（分）＝这张票的基准售价，误差闸比的就是它
 * @param weekendPriceCent  周末售价（分，可空）
 * @param holidayPriceCent  节假日售价（分，可空）
 */
public record TicketTypePricingRequest(
        String idempotencyKey,
        String reason,
        BigDecimal comboDiscountRate,
        Long weekdayPriceCent,
        Long weekendPriceCent,
        Long holidayPriceCent
) {
}
