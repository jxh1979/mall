package com.complex.platform.ticketing;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record TicketDynamicPricingRuleRecord(
        String id,
        String ticketTypeId,
        String ruleName,
        int priority,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String salesChannel,
        String adjustmentType,
        BigDecimal adjustmentRate,
        Long adjustmentAmountCent,
        Long minPriceCent,
        Long maxPriceCent,
        String status,
        Map<String, Object> conditionRule,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        Instant createdAt,
        Instant updatedAt
) {
}
