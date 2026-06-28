package com.complex.platform.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** 储值消费请求（余额扣减，不足拒）。B5 边界校验：幂等键非空、金额正数（与 service 层 double-spend 守恒一致，前置拦截）。 */
public record StoredValueConsumeRequest(
        @NotBlank String idempotencyKey,
        String projectId,
        @NotNull @Positive Long amountCent,
        String reason
) {
}
