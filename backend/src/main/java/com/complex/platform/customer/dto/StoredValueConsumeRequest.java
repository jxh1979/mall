package com.complex.platform.customer.dto;

// 文件版本：V0.01

import jakarta.validation.constraints.NotBlank;

/** 客人订单储值支付：指定本人卡，金额从路径中的真实订单读取。 */
public record StoredValueConsumeRequest(
        @NotBlank String idempotencyKey,
        @NotBlank String accountId,
        @NotBlank String phone,
        @NotBlank String smsCode
) {
}
