package com.complex.platform.leasing.dto;

import java.math.BigDecimal;

/** 招商线索创建请求（V161 重建）。source 4 类、brandId 用于品牌库去重与黑名单拦截。 */
public record LeasingLeadRequest(
        String contactName,
        String contactPhone,
        String source,
        String intendedBusinessType,
        String intendedSpaceId,
        BigDecimal intendedAreaSqm,
        String brandId,
        String idempotencyKey
) {
}
