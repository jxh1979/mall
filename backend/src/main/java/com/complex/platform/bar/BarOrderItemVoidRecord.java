package com.complex.platform.bar;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.List;

/**
 * V091 退菜退酒流水（PRD §10.1）。区分出品前撤单（void_type=cancel，不计库存损耗）与
 * 出品后退酒（void_type=return，按 returnToStock 回补库存或计损耗，资金从账单扣减）。
 */
public record BarOrderItemVoidRecord(
        String id,
        String orderItemId,
        String orderId,
        String barId,
        @JsonIgnore String merchantId,
        String voidType,
        int quantity,
        long amountCent,
        boolean wasDelivered,
        boolean returnToStock,
        String inventoryMovementId,
        String billId,
        String billAdjustmentId,
        String reason,
        String requestedByUserId,
        boolean approvalRequired,
        String status,
        String idempotencyKey,
        List<String> evidenceAttachmentIds,
        Instant createdAt,
        // V115：审批回写留痕。status=approved/rejected 时填充审批人/时点/理由；pending/applied 态为空。
        String reviewedByUserId,
        Instant reviewedAt,
        String reviewReason
) {
}
