package com.complex.platform.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** 储值充值请求（客人端，绑支付意图）。amountCent=充值金额（分）；channel=支付渠道。B5 边界校验：幂等键非空、金额正数。 */
public record StoredValueTopupRequest(
        @NotBlank String idempotencyKey,
        String projectId,
        @NotNull @Positive Long amountCent,
        String channel
) {
}
