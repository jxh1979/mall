package com.complex.platform.ticketing.dto;

import java.time.LocalDate;
import java.util.List;

public record TicketOrderRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String ticketTypeId,
        String priceRuleId,
        Integer quantity,
        LocalDate visitDate,
        String salesChannel,
        String buyerPhone,
        String customerId
) {
}
