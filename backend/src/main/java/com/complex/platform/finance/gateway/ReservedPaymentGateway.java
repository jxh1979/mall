package com.complex.platform.finance.gateway;

/**
 * 渠道接缝实现：能力开关 {@code payment.gateway.enabled} 开但当前无真实厂商接入时使用。
 *
 * <p>这是为真实渠道 SDK 预留的接缝。当前返回 {@code NOT_CONFIGURED}（不崩、不调外部），
 * 接入真实厂商（微信/支付宝/银联等）后，在此实现内调用对应 SDK，并返回真实
 * {@code SUCCEEDED/PENDING/FAILED} 及 gatewayProvider/gatewayRef。
 */
public final class ReservedPaymentGateway implements PaymentGatewayPort {

    private static final String MSG = "payment gateway enabled but no real provider adapter is configured "
            + "(integration_provider_adapters adapter_type='payment'); seam reserved, real SDK pending";

    @Override
    public PaymentGatewayResult initiatePayment(PaymentGatewayRequest request) {
        return PaymentGatewayResult.notConfigured(MSG);
    }

    @Override
    public PaymentGatewayResult queryStatus(String gatewayRef) {
        return PaymentGatewayResult.notConfigured(MSG);
    }

    @Override
    public PaymentGatewayResult refund(PaymentGatewayRequest request) {
        return PaymentGatewayResult.notConfigured(MSG);
    }

    @Override
    public PaymentGatewayResult autoRenewCharge(PaymentGatewayRequest request) {
        return PaymentGatewayResult.notConfigured(MSG);
    }
}
