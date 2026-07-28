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
        String reviewReason,
        // V345（P0-C/FLOW-07·BAR-VOID-A1）审批闸留痕：
        /** 审批闸模式：immediate=即时生效（未出品/低价值）；approval_first=先审后行（已出品高价值）。 */
        String approvalMode,
        /** 双证据之一：结构化原因码（approval_first 必填；immediate 可空）。另一证据为 evidenceAttachmentIds。 */
        String reasonCode,
        /** 判定当刻的高价值阈值快照（分）。阈值后台可配会变，留快照才能事后举证「当时按什么口径判的」。 */
        Long highValueThresholdCent,
        /** 库存/账单副作用真正落地时点：immediate=发起时刻；approval_first=审批通过时刻；未落地=null。 */
        Instant effectsAppliedAt
) {
}
