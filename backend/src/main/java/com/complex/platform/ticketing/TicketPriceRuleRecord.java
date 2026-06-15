package com.complex.platform.ticketing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record TicketPriceRuleRecord(
        String id,
        String ticketTypeId,
        String variantType,
        String priceMode,
        Long weekdayPriceCent,
        Long weekendPriceCent,
        Long holidayPriceCent,
        BigDecimal discountRate,
        String channelId,
        String currency,
        String status,
        Map<String, Object> eligibilityRule,
        Instant createdAt,
        Instant updatedAt
) {
}
