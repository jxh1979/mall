package com.complex.platform.amusement.dto;

/** CG-08b 配置补偿有效期（天）：值与上下限须同时填写，缺任一不得保存。 */
public record CompensationPolicyRequest(
        String idempotencyKey,
        Integer validityDays,
        Integer minDays,
        Integer maxDays
) {
}
