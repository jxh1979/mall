package com.complex.platform.finance.dto;

import java.util.List;

/**
 * R5 T7：补录审批决定（approveAdjustment）。decision ∈ approve / reject。
 * approve 才真正写金额变动入账；reject 不改账。
 */
public record ArApAdjustmentDecisionRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String decision
) {
}
