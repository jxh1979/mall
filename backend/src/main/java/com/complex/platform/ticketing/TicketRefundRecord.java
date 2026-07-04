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
        Instant updatedAt,
        // V220：受理退款的销售商户（=原单 merchant_id；平台直营单为 null）。商户级退款分量直连用。
        String merchantId
) {
}
