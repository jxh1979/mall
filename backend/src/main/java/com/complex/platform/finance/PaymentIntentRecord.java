package com.complex.platform.finance;

// 文件版本：V0.02

import com.complex.platform.finance.gateway.PaymentClientLaunch;
import java.time.Instant;

/**
 * 支付意图记录。
 *
 * <p>渠道预留字段（gatewayProvider/gatewayRef/gatewayStatus，V093 落库）记录与真实支付渠道的交互预留：
 * 支付渠道开关关时为占位值（reserved / null），开关开经 {@code PaymentGatewayPort} 后由适配器回填。
 * ROOT-03-R18：launch＝App/小程序拉起参数，同 paymentUrl 只随发起响应返回、不落库，渠道未配置时为空。
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
        Instant updatedAt,
        String paymentUrl,
        PaymentClientLaunch launch
) {
    /** 拉起链接随本次响应返回，不作为支付成功凭据。 */
    public PaymentIntentRecord(String id, String businessOrderType, String businessOrderId,
            long amountCent, String currency, String channel, String status,
            String gatewayProvider, String gatewayRef, String gatewayStatus, Instant createdAt, Instant updatedAt) {
        this(id, businessOrderType, businessOrderId, amountCent, currency, channel, status,
                gatewayProvider, gatewayRef, gatewayStatus, createdAt, updatedAt, null, null);
    }

    public PaymentIntentRecord(String id, String businessOrderType, String businessOrderId,
            long amountCent, String currency, String channel, String status,
            String gatewayProvider, String gatewayRef, String gatewayStatus, Instant createdAt, Instant updatedAt,
            String paymentUrl) {
        this(id, businessOrderType, businessOrderId, amountCent, currency, channel, status,
                gatewayProvider, gatewayRef, gatewayStatus, createdAt, updatedAt, paymentUrl, null);
    }

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
