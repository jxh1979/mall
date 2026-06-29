package com.complex.platform.finance.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.List;

public record OfflinePaymentConfirmRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String paymentId,
        @NotNull @Positive Long amountCent,
        String paymentMethod,
        Instant confirmedAt,
        // F1 财务统一台账：可选——确认线下收款时显式指明所结算的应收应付台账项。
        // 提供时回写 ar_ap_items.paid/unpaid/status + 登记 ar_ap_payments；不提供时维持纯平台收款确认（向后兼容）。
        String arApItemId
) {
    /** 向后兼容构造器（无 arApItemId）：既有调用方/测试不变。 */
    public OfflinePaymentConfirmRequest(
            String idempotencyKey,
            String operatorNote,
            List<String> evidenceAttachmentIds,
            String paymentId,
            Long amountCent,
            String paymentMethod,
            Instant confirmedAt
    ) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, paymentId, amountCent, paymentMethod, confirmedAt, null);
    }
}
