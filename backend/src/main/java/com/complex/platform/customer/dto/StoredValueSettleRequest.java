package com.complex.platform.customer.dto;

/** 储值充值入账请求（支付成功后，校验支付意图已 succeeded 再入账）。 */
public record StoredValueSettleRequest(
        String projectId,
        String paymentIntentId
) {
}
