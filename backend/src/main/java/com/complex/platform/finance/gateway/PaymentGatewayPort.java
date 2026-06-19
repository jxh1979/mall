package com.complex.platform.finance.gateway;

/**
 * 支付渠道端口（端口/适配器模式，用户口径：第三方/支付一律"预留接口 + 能力开关默认关"）。
 *
 * <p>所有真实第三方支付交互（收款下单、状态查询、退款打款、自动续费扣款）经此端口抽象，
 * 业务侧（finance 支付意图 / 退款、储值退款打款、付费会员自动续费扣款）只依赖本接口，
 * 不直接耦合任何厂商 SDK。
 *
 * <p>路由由 {@link PaymentGatewayRouter} 按能力开关 {@code payment.gateway.enabled} 决定：
 * <ul>
 *   <li>开关关（默认）→ {@link DisabledPaymentGateway}：返回 {@code RESERVED}，
 *       不调外部，业务回退现有内部/占位结算（人工/内部确认，不阻塞）。</li>
 *   <li>开关开 → {@link ReservedPaymentGateway}：当前无真实厂商=占位实现，返回 {@code NOT_CONFIGURED}
 *       （留接缝，接入真实 SDK 后替换此实现）。</li>
 * </ul>
 */
public interface PaymentGatewayPort {

    /** 发起收款（对应支付意图）。 */
    PaymentGatewayResult initiatePayment(PaymentGatewayRequest request);

    /** 查询渠道侧支付状态。 */
    PaymentGatewayResult queryStatus(String gatewayRef);

    /** 退款打款（储值退款 / finance 退款）。 */
    PaymentGatewayResult refund(PaymentGatewayRequest request);

    /** 自动续费扣款（付费会员到期自动续费）。 */
    PaymentGatewayResult autoRenewCharge(PaymentGatewayRequest request);
}
