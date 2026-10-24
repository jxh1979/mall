package com.complex.platform.customer.dto;

// 文件版本：V0.01

/**
 * 操作员裁决第十三批第 11 条「客人端自助申请」：客人对本人名下的一张票（父票实例，即「我的票」里的 parent_ticket_id）申请退票。
 * 理由固定为「票种规则明确允许」，客人不填退款责任主体等员工字段。
 */
public record CustomerTicketRefundRequest(
        String idempotencyKey,
        String ticketInstanceId
) {
}
