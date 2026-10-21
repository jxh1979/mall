package com.complex.platform.ticketing;

// 文件版本：V0.01

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
        String merchantId,
        // TK-05（V526，BSM:804 五种情形 / 最高权限逐笔放行 / 通票安全例外；BSM:806 记录退款原因、责任主体、适用规则版本）：
        // 退款理由闭集码、责任主体（business_entities.id）、申请时票种是否允许退款的快照。旧行为空。
        String reasonCode,
        String responsibleEntityId,
        Boolean ticketTypeRefundableSnapshot
) {
    /** V526 前的 12 参构造（既有测试/工具链）：理由、责任主体与规则快照为空。 */
    public TicketRefundRecord(String id, String targetId, long refundAmountCent, String reason, String refundChannel,
            String status, String requestedBy, String approvedBy, Instant approvedAt, Instant createdAt, Instant updatedAt,
            String merchantId) {
        this(id, targetId, refundAmountCent, reason, refundChannel, status, requestedBy, approvedBy, approvedAt,
                createdAt, updatedAt, merchantId, null, null, null);
    }

    /** 状态流转（批准/驳回/拨付确认）：其余字段原样带过。 */
    public TicketRefundRecord withStatus(String status, String approvedBy, Instant approvedAt, Instant updatedAt) {
        return new TicketRefundRecord(id, targetId, refundAmountCent, reason, refundChannel, status, requestedBy, approvedBy,
                approvedAt, createdAt, updatedAt, merchantId, reasonCode, responsibleEntityId, ticketTypeRefundableSnapshot);
    }
}
