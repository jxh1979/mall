package com.complex.platform.ticketing;

import java.time.Instant;

public record TicketRefundRecord(
        String id,
        String targetId,
        long refundAmountCent,
        String reason,
        String refundChannel,
        String status,
        // A2 修复（V157）：退款发起人留痕。旧数据/未开分级授权时为 null。
        String requestedBy,
        String approvedBy,
        Instant approvedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
