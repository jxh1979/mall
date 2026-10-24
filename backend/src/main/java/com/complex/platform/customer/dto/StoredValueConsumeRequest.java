package com.complex.platform.customer.dto;

// 文件版本：V0.02

import jakarta.validation.constraints.NotBlank;

/**
 * 客人订单储值支付：指定本人卡，金额从路径中的真实订单读取。ROOT-03-R20 组合支付：amountCent 可选，只付一部分（不超过待付金额）、
 * 其余在线补足；不填＝付清待付金额。
 */
public record StoredValueConsumeRequest(
        @NotBlank String idempotencyKey,
        @NotBlank String accountId,
        @NotBlank String phone,
        @NotBlank String smsCode,
        Long amountCent
) {
}
