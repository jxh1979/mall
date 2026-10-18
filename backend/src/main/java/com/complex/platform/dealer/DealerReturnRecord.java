package com.complex.platform.dealer;

import java.time.Instant;

/**
 * 经销退货单（ENUM:174 十态：pending_approval 待审批 / rejected 已驳回 / pending_receipt 待收货 / inspecting 验收中 / pending_reversal 待红冲 /
 * pending_refund 待退款 / refunding 退款中 / refund_failed 退款失败 / partially_completed 部分完成 / completed 已完成）。
 * reasonType：unopened_resalable 未拆封可复售 / quality_issue 质量问题（BSM:1240）。
 */
public record DealerReturnRecord(
        String id,
        String returnNo,
        String orderId,
        String dealerId,
        String salesEntityId,
        String status,
        String reasonType,
        int requestedQuantity,
        int acceptedQuantity,
        long acceptedAmountCent,
        long offsetReceivableCent,
        long refundCent,
        long refundedCent,
        String refundReviewId,
        boolean refundApproved,
        String returnFreightBearer,
        Boolean firstLegFreightRefundable,
        boolean freightConfirmed,
        String approvedBy,
        Instant approvedAt,
        String receivedBy,
        Instant receivedAt,
        String inspectedBy,
        Instant inspectedAt,
        String refundExecutedBy,
        Instant refundExecutedAt,
        String refundReferenceNo,
        String failureReason,
        String note,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
