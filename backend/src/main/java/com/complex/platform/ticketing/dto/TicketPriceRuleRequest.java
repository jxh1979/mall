package com.complex.platform.ticketing.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record TicketPriceRuleRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String ticketTypeId,
        String variantType,
        String priceMode,
        Long weekdayPriceCent,
        Long weekendPriceCent,
        Long holidayPriceCent,
        BigDecimal discountRate,
        String channelId,
        Map<String, Object> eligibilityRule,
        // 第十八批 AW 段 338-R1：price_mode=amount_delta 时的加减金额（分，负数为减免、正数为加价）。
        Long adjustmentAmountCent
) {
    /** 向后兼容构造：338-R1 前 12 参 → 加减金额为空。 */
    public TicketPriceRuleRequest(String idempotencyKey, String operatorNote, List<String> evidenceAttachmentIds, String ticketTypeId,
            String variantType, String priceMode, Long weekdayPriceCent, Long weekendPriceCent, Long holidayPriceCent,
            BigDecimal discountRate, String channelId, Map<String, Object> eligibilityRule) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, ticketTypeId, variantType, priceMode, weekdayPriceCent,
                weekendPriceCent, holidayPriceCent, discountRate, channelId, eligibilityRule, null);
    }
}
