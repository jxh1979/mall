package com.complex.platform.finance.dto;

import java.util.List;

/**
 * R5 T7：锁定后补录审批申请（requestAdjustment）。
 * arApItemId 指向已锁定台账项；adjustmentAmountCent 为带符号补录变动额（正=增、负=减）。
 * 第十八批 Z 段第 186 条 186-R1：kind 缺省 amount（金额调整）；payment＝收付款补录，金额为正、paymentMethod 取线下四种，批准后登记收付款。
 * 186-R2：refund＝退款补录，金额为正，不填 paymentMethod（照原收款记录），批准后按原收款走原路退款。
 */
public record ArApAdjustmentRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String arApItemId,
        Long adjustmentAmountCent,
        String reason,
        String kind,
        String paymentMethod
) {
    /** 金额调整（kind 缺省）的既有写法。 */
    public ArApAdjustmentRequest(String idempotencyKey, String operatorNote, List<String> evidenceAttachmentIds,
            String arApItemId, Long adjustmentAmountCent, String reason) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, arApItemId, adjustmentAmountCent, reason, null, null);
    }
}
