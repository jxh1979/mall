package com.complex.platform.ticketing.dto;

import java.util.List;

public record TicketTypeEntitlementRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String entitlementTemplateId,
        Integer quantity,
        Integer totalTimes,
        Long valueAmountCent,
        String effectiveRule
) {
}
