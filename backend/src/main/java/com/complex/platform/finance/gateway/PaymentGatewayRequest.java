package com.complex.platform.finance.gateway;

/**
 * 支付渠道请求（端口入参，厂商无关）。
 *
 * @param businessOrderType 业务单据类型（stored_value_refund / finance_refund 等）
 * @param businessOrderId   业务单据 id（支付意图 id / 账户 id 等）
 * @param amountCent        金额（分，正数）
 * @param currency          币种（默认 CNY）
 * @param channel           渠道标识（wechat/alipay/... 仅作路由提示，真实路由由适配器决定）
 * @param gatewayRef        渠道侧引用（查询/退款时回填）
 * @param payeeMerchantId   AUD-013 收款商户（null=平台收款）：merchant_prepaid 卡充值等资金直达商户的场景必须携带，
 *                          真实适配器据此路由收款主体，杜绝「支付进平台、账务认商户预收」错配
 * @param fundMode          AUD-013 资金模式（merchant_prepaid=商户预收直达商户 / platform_collect=平台代收；null=平台自营）
 */
public record PaymentGatewayRequest(
        String businessOrderType,
        String businessOrderId,
        long amountCent,
        String currency,
        String channel,
        String gatewayRef,
        String payeeMerchantId,
        String fundMode
) {
    /** 兼容构造（无收款主体/资金模式=平台收款）：既有退款/查询调用方沿用。 */
    public PaymentGatewayRequest(String businessOrderType, String businessOrderId,
                                 long amountCent, String currency, String channel, String gatewayRef) {
        this(businessOrderType, businessOrderId, amountCent, currency, channel, gatewayRef, null, null);
    }

    public static PaymentGatewayRequest of(String businessOrderType, String businessOrderId,
                                           long amountCent, String channel) {
        return new PaymentGatewayRequest(businessOrderType, businessOrderId, amountCent, "CNY", channel, null, null, null);
    }
}
