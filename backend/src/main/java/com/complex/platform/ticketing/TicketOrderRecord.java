package com.complex.platform.ticketing;

import java.time.Instant;
import java.time.LocalDate;

public record TicketOrderRecord(
        String id,
        String orderNo,
        String customerId,
        String ticketTypeId,
        String priceRuleId,
        int quantity,
        LocalDate visitDate,
        String salesChannel,
        String buyerPhone,
        long amountCent,
        String currency,
        String payStatus,
        String issueStatus,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
