package com.complex.platform.ticketing;

// 文件版本：V0.01

import java.time.Instant;

/**
 * 操作员裁决第十三批第 11 条：客人本人退票申请的安全投影——申请对应的票、金额（分）与状态（requested 已申请、approved 已通过待原路打款、
 * rejected 已驳回、success 成功）；不含审批人、发起人、商户与责任主体等内部字段。
 */
public record CustomerTicketRefundResponse(
        String id,
        String ticketInstanceId,
        String ticketNo,
        String ticketName,
        long refundAmountCent,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
