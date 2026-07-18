package com.complex.platform.finance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record PaymentIntentRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String businessOrderType,
        String businessOrderId,
        // 审计 A3/T1.3 边界防御纵深：金额声明式校验（服务层 requirePositive 仍是主闸，此为更早失败的双保险）。
        @NotNull @Positive Long amountCent,
        String currency,
        String channel,
        // 跨模块闭环②a：现场/工作机发起支付意图时携带的操作设备标识（MDM 门控真实启用）。
        // 可空（尾部，向后兼容）：现场工作机收款应传 deviceId → 触发 MDM 硬门控；
        // 后台/服务端发起无设备上下文 → 留空，门控安全跳过（不伪造假值）。
        String deviceId,
        // AUD-013 资金归属契约（可空，尾部向后兼容）：收款商户（null=平台收款）+ 资金模式
        // （merchant_prepaid=商户预收直达商户 / platform_collect=平台代收），透传支付网关路由收款主体。
        String payeeMerchantId,
        String fundMode
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
                businessOrderId, amountCent, currency, channel, null, null, null);
    }

    /** 向后兼容构造（带 deviceId、无资金归属=平台收款）：保留既有 9 参调用方。 */
    public PaymentIntentRequest(
            String idempotencyKey,
            String operatorNote,
            List<String> evidenceAttachmentIds,
            String businessOrderType,
            String businessOrderId,
            Long amountCent,
            String currency,
            String channel,
            String deviceId
    ) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, businessOrderType,
                businessOrderId, amountCent, currency, channel, deviceId, null, null);
    }
}
