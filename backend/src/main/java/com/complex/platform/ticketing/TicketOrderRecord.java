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
        String dynamicPricingRuleId,
        long baseAmountCent,
        long dynamicAdjustmentCent,
        String currency,
        String payStatus,
        String issueStatus,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public TicketOrderRecord(
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
        this(
                id,
                orderNo,
                customerId,
                ticketTypeId,
                priceRuleId,
                quantity,
                visitDate,
                salesChannel,
                buyerPhone,
                amountCent,
                null,
                amountCent,
                0L,
                currency,
                payStatus,
                issueStatus,
                status,
                createdAt,
                updatedAt
        );
    }
}
