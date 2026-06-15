package com.complex.platform.ticketing;

import java.time.LocalDate;
import java.util.Map;

public record TicketDynamicPricingSimulationRequest(
        String idempotencyKey,
        String ticketTypeId,
        LocalDate visitDate,
        String salesChannel,
        Map<String, Object> context
) {
}
