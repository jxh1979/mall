package com.complex.platform.ticketing.dto;

import java.util.List;

public record TicketTypeRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String ticketName,
        String baseCalendarPriceMode,
        Long weekdayPriceCent,
        Long weekendPriceCent,
        Long holidayPriceCent,
        List<String> entitlementTemplateIds,
        Boolean stockRequired,
        String status
) {
}
