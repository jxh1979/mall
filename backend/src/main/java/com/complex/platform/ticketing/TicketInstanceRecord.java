package com.complex.platform.ticketing;

import java.time.Instant;

/**
 * AUD-066 父票实例：订单里每买 1 张票 = 1 条（四人票买 2 张 = 2 条）。除状态/首用时刻/持有人外不可变。
 * 方案2（整票退款）锚 status/first_used_at；方案3（转赠）锚 current_holder_customer_id + slot。
 */
public record TicketInstanceRecord(
        String id,
        String ticketNo,
        String orderId,
        String ticketTypeId,
        int seqInOrder,
        int numPeople,
        int totalTimes,
        String buyerCustomerId,
        String currentHolderCustomerId,
        String status,
        Instant firstUsedAt,
        String firstSessionKey,
        Instant createdAt,
        Instant updatedAt
) {
}
