package com.complex.platform.finance.gateway;

/**
 * 默认实现：能力开关 {@code payment.gateway.enabled} 关时使用。
 * 不调任何外部渠道，统一返回 {@code RESERVED}，业务回退现有内部/占位结算（不阻塞）。
 */
public final class DisabledPaymentGateway implements PaymentGatewayPort {

    private static final String MSG = "payment gateway capability is disabled (payment.gateway.enabled=false); "
            + "falling back to internal/placeholder settlement";

    @Override
    public PaymentGatewayResult initiatePayment(PaymentGatewayRequest request) {
        return PaymentGatewayResult.reserved(MSG);
    }

    @Override
    public PaymentGatewayResult queryStatus(String gatewayRef) {
        return PaymentGatewayResult.reserved(MSG);
    }

    @Override
    public PaymentGatewayResult refund(PaymentGatewayRequest request) {
        return PaymentGatewayResult.reserved(MSG);
    }

    @Override
    public PaymentGatewayResult autoRenewCharge(PaymentGatewayRequest request) {
        return PaymentGatewayResult.reserved(MSG);
    }
}
