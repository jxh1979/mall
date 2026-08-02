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

}
