package com.complex.platform.finance.dto;

import java.util.List;

public record PaymentIntentRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String businessOrderType,
        String businessOrderId,
        Long amountCent,
        String currency,
        String channel,
        // 跨模块闭环②a：现场/工作机发起支付意图时携带的操作设备标识（MDM 门控真实启用）。
        // 可空（尾部，向后兼容）：现场工作机收款应传 deviceId → 触发 MDM 硬门控；
        // 后台/服务端发起无设备上下文 → 留空，门控安全跳过（不伪造假值）。
        String deviceId
) {
    /**
     * 向后兼容构造（无 deviceId）：供后台/服务端发起的支付意图（无现场设备上下文）使用，
     * deviceId 置 null → 门控安全跳过。保留旧调用方（如储值充值/会员开通）无需改动。
     */
    public PaymentIntentRequest(
            String idempotencyKey,
            String operatorNote,
            List<String> evidenceAttachmentIds,
            String businessOrderType,
            String businessOrderId,
            Long amountCent,
            String currency,
            String channel
    ) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, businessOrderType,
                businessOrderId, amountCent, currency, channel, null);
    }
}
