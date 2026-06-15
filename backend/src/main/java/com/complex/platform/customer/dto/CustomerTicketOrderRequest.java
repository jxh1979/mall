package com.complex.platform.customer.dto;

import java.time.LocalDate;

public record CustomerTicketOrderRequest(
        String idempotencyKey,
        String ticketTypeId,
        String priceRuleId,
        Integer quantity,
        LocalDate visitDate,
        String contactPhone,
        String buyerNote
) {
}
