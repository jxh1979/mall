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
        String merchantId,
        // V231 强制口径：支付成功时刻（pay_status→paid 时写入）；未支付为 null。与 createdAt（下单时间）严格区分。
        Instant paidAt,
        // AUD-076 单店票挂酒吧：下单时从票种快照的酒吧商户 id；null=通票/平台票。与 merchantId（受理商户）语义正交。
        String barId
) {
    // V231 兼容构造器（20 参，含 merchantId 无 paidAt）：paidAt 默认 null（既有非支付确认调用点零改动）。
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
            Instant updatedAt,
            String merchantId
    ) {
        this(id, orderNo, customerId, ticketTypeId, priceRuleId, quantity, visitDate, salesChannel,
                buyerPhone, amountCent, dynamicPricingRuleId, baseAmountCent, dynamicAdjustmentCent,
                currency, payStatus, issueStatus, status, createdAt, updatedAt, merchantId, null, null);
    }

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
