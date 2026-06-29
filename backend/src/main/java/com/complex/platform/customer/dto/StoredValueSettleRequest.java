package com.complex.platform.customer.dto;

import jakarta.validation.constraints.NotBlank;

/** 储值充值入账请求（支付成功后，校验支付意图已 succeeded 再入账）。B5 边界校验：支付意图号非空。 */
public record StoredValueSettleRequest(
        String projectId,
        String merchantId,
        @NotBlank String paymentIntentId
) {
}
