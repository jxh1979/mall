package com.complex.platform.finance;

import java.time.Instant;

/**
 * 支付意图记录。
 *
 * <p>渠道预留字段（gatewayProvider/gatewayRef/gatewayStatus，V093 落库）记录与真实支付渠道的交互预留：
 * 支付渠道开关关时为占位值（reserved / null），开关开经 {@code PaymentGatewayPort} 后由适配器回填。
 */
public record PaymentIntentRecord(
        String id,
        String businessOrderType,
        String businessOrderId,
        long amountCent,
        String currency,
        String channel,
        String status,
        String gatewayProvider,
        String gatewayRef,
        String gatewayStatus,
        Instant createdAt,
        Instant updatedAt
) {
    /** 便捷构造：无渠道预留信息（占位/内部结算路径，向后兼容旧 9 参签名）。 */
    public PaymentIntentRecord(
            String id,
            String businessOrderType,
            String businessOrderId,
            long amountCent,
            String currency,
            String channel,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, businessOrderType, businessOrderId, amountCent, currency, channel, status,
                null, null, null, createdAt, updatedAt);
    }
}
