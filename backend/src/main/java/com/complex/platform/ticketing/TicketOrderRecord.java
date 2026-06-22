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
        Instant updatedAt,
        // 行级隔离（V135）：下单受理/销售商户；平台下单为 null（平台官方票务口径）。列表读取按此 scope 过滤。
        String merchantId
) {
    // 兼容构造器（16 参，无 dynamicPricing/merchantId）：merchantId 默认 null。
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
                updatedAt,
                null
        );
    }

    // 兼容构造器（19 参，含 dynamicPricing，无 merchantId）：merchantId 默认 null（既有调用点零改动）。
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
                dynamicPricingRuleId,
                baseAmountCent,
                dynamicAdjustmentCent,
                currency,
                payStatus,
                issueStatus,
                status,
                createdAt,
                updatedAt,
                null
        );
    }
}
