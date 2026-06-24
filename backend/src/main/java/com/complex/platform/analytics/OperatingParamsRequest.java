package com.complex.platform.analytics;

import java.math.BigDecimal;

/** 经营参数 upsert 请求（T4.2）：按 projectId 设可经营面积（㎡，必填且 >0）。 */
public record OperatingParamsRequest(
        String projectId,
        BigDecimal operatingAreaSqm,
        String status,
        String idempotencyKey
) {
}
