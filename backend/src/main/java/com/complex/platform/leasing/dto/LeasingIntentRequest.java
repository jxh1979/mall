package com.complex.platform.leasing.dto;

import java.math.BigDecimal;
import java.util.Map;

/** 由线索转意向的创建请求（V161 重建）：lead 推进到「已转意向」时生成意向（stage 起始 initial_contact）。 */
public record LeasingIntentRequest(
        String leadId,
        String intendedSpaceId,
        BigDecimal intendedAreaSqm,
        Map<String, Object> rentTerms,
        String idempotencyKey
) {
}
