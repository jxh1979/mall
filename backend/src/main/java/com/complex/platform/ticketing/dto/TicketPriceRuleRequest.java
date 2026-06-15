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
        Map<String, Object> eligibilityRule
) {
}
