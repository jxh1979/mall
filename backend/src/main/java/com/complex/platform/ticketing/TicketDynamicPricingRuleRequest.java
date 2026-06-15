package com.complex.platform.ticketing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record TicketDynamicPricingRuleRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String ticketTypeId,
        String ruleName,
        Integer priority,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String salesChannel,
        String adjustmentType,
        BigDecimal adjustmentRate,
        Long adjustmentAmountCent,
        Long minPriceCent,
        Long maxPriceCent,
        Map<String, Object> conditionRule
) {
}
