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
        // F-TXN-03 票实例退款（2026-06-29 裁决）：要退该订单下哪几张未核销票（issued_entitlements ID）；
        // 不传/为空=退该订单全部可退（未核销）票；已核销票传入将按票实例拒退 TICKET_ENTITLEMENT_VERIFIED_NO_REFUND。
        List<String> entitlementIds
) {
    /** 向后兼容构造：不指定票实例（= 退订单全部可退票）。供既有调用与单测，不改既有用例。 */
    public TicketRefundRequest(
            String idempotencyKey,
            String operatorNote,
            List<String> evidenceAttachmentIds,
            String targetId,
            Long refundAmountCent,
            String reason,
            String refundChannel) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, targetId, refundAmountCent, reason, refundChannel, null);
    }
}
