package com.complex.platform.finance.gateway;

/**
 * 支付渠道请求（端口入参，厂商无关）。
 *
 * @param businessOrderType 业务单据类型（stored_value_refund / membership_auto_renew / finance_refund 等）
 * @param businessOrderId   业务单据 id（支付意图 id / 账户 id / 会员 id 等）
 * @param amountCent        金额（分，正数）
 * @param currency          币种（默认 CNY）
 * @param channel           渠道标识（wechat/alipay/... 仅作路由提示，真实路由由适配器决定）
 * @param gatewayRef        渠道侧引用（查询/退款时回填）
 */
public record PaymentGatewayRequest(
        String businessOrderType,
        String businessOrderId,
        long amountCent,
        String currency,
        String channel,
        String gatewayRef
) {
    public static PaymentGatewayRequest of(String businessOrderType, String businessOrderId,
                                           long amountCent, String channel) {
        return new PaymentGatewayRequest(businessOrderType, businessOrderId, amountCent, "CNY", channel, null);
    }
}
