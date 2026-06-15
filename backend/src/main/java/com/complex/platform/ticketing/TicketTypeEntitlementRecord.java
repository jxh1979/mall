package com.complex.platform.ticketing;

import java.time.Instant;

public record TicketTypeEntitlementRecord(
        String id,
        String ticketTypeId,
        String entitlementTemplateId,
        int quantity,
        int totalTimes,
        Long valueAmountCent,
        String effectiveRule,
        Instant createdAt,
        Instant updatedAt
) {
}
