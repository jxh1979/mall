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
        Instant updatedAt,
        // AUD-076 单店票挂酒吧：出票时从订单快照的酒吧商户 id；null=通票/平台票。
        String barId
) {
    // ponytail: AUD-076 前的 14 参构造点（既有测试/直发）不带 barId，默认 null（通票）。
    public TicketInstanceRecord(
            String id, String ticketNo, String orderId, String ticketTypeId, int seqInOrder, int numPeople,
            int totalTimes, String buyerCustomerId, String currentHolderCustomerId, String status,
            Instant firstUsedAt, String firstSessionKey, Instant createdAt, Instant updatedAt) {
        this(id, ticketNo, orderId, ticketTypeId, seqInOrder, numPeople, totalTimes, buyerCustomerId,
                currentHolderCustomerId, status, firstUsedAt, firstSessionKey, createdAt, updatedAt, null);
    }
}
