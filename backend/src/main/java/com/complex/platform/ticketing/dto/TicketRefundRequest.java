package com.complex.platform.ticketing.dto;

import java.util.List;

public record TicketRefundRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String targetId,
        Long refundAmountCent,
        String reason,
        String refundChannel,
        // 方案2 整票退款（AUD-006）已退役：退款单位改为父票实例（见 ticketInstanceIds）。
        // 本字段仅为契约过渡保留；传入非空即 400 REFUND_UNIT_IS_TICKET（退款单位为整张票，请按票选择）。
        List<String> entitlementIds,
        // 方案2 整票退款（AUD-006）：要退该订单下哪几张父票实例（ticket_instances ID）；不传/空=退该订单全部“完全未核销”的票。
        // 金额由服务端按各实例出票时定格的实付分摊额算定；父票任一权益已核销则整张不可退。
        List<String> ticketInstanceIds
) {
    /** 向后兼容构造：不指定票（= 退订单全部完全未核销票）。供工具链/整单退。 */
    public TicketRefundRequest(
            String idempotencyKey,
            String operatorNote,
            List<String> evidenceAttachmentIds,
            String targetId,
            Long refundAmountCent,
            String reason,
            String refundChannel) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, targetId, refundAmountCent, reason, refundChannel, null, null);
    }

    /** 过渡构造：旧 entitlement_ids 入参（运行时按整票口径拒 400）；ticketInstanceIds 缺省。 */
    public TicketRefundRequest(
            String idempotencyKey,
            String operatorNote,
            List<String> evidenceAttachmentIds,
            String targetId,
            Long refundAmountCent,
            String reason,
            String refundChannel,
            List<String> entitlementIds) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, targetId, refundAmountCent, reason, refundChannel,
                entitlementIds, null);
    }

    /** 整票退款构造：按父票实例选票（entitlementIds 恒 null）。 */
    public static TicketRefundRequest ofTicketInstances(
            String idempotencyKey,
            String operatorNote,
            List<String> evidenceAttachmentIds,
            String targetId,
            Long refundAmountCent,
            String reason,
            String refundChannel,
            List<String> ticketInstanceIds) {
        return new TicketRefundRequest(idempotencyKey, operatorNote, evidenceAttachmentIds, targetId,
                refundAmountCent, reason, refundChannel, null, ticketInstanceIds);
    }
}
