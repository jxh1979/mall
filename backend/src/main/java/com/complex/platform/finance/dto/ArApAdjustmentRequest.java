package com.complex.platform.finance.dto;

import java.util.List;

/**
 * R5 T7：锁定后补录审批申请（requestAdjustment）。
 * arApItemId 指向已锁定台账项；adjustmentAmountCent 为带符号补录变动额（正=增、负=减）。
 */
public record ArApAdjustmentRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String arApItemId,
        Long adjustmentAmountCent,
        String reason
) {
}
