package com.complex.platform.ticketing;

import java.time.LocalDate;

public record TicketDynamicPricingSimulationResponse(
        String ticketTypeId,
        LocalDate visitDate,
        String salesChannel,
        long basePriceCent,
        long adjustmentCent,
        long finalPriceCent,
        String matchedRuleId,
        String explanation
) {
}
