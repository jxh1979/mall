package com.complex.platform.finance.dto;

import java.time.Instant;
import java.util.List;

public record ArApPaymentRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        Long amountCent,
        String paymentMethod,
        Instant paymentTime,
        String bankFlowRef,
        // 跨模块闭环②a：现场/工作机发起收付款时携带的操作设备标识（MDM 门控真实启用）。
        // 可空（尾部，向后兼容）：现场工作机收款应传 deviceId → 触发 MDM 硬门控；
        // 后台台账类登记收付款无设备上下文 → 留空，门控安全跳过（不伪造假值）。
        String deviceId
) {
    /**
     * 向后兼容构造（无 deviceId）：供后台台账类登记收付款（无现场设备上下文）使用，
     * deviceId 置 null → 门控安全跳过。保留历史调用方无需改动。
     */
    public ArApPaymentRequest(
            String idempotencyKey,
            String operatorNote,
            List<String> evidenceAttachmentIds,
            Long amountCent,
            String paymentMethod,
            Instant paymentTime,
            String bankFlowRef
    ) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, amountCent,
                paymentMethod, paymentTime, bankFlowRef, null);
    }
}
