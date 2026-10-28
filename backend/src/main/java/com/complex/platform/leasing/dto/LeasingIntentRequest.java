package com.complex.platform.leasing.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 由线索转意向的创建请求（V161 重建）：lead 推进到「已转意向」时生成意向（stage 起始 initial_contact）。
 * blacklistExceptionId：拉黑前既有线索转意向被拦时，凭本人已批准的黑名单例外放行单放行（第十八批第 30 条，PGM L502），未被拦时忽略。
 */
public record LeasingIntentRequest(
        String leadId,
        String intendedSpaceId,
        BigDecimal intendedAreaSqm,
        Map<String, Object> rentTerms,
        String idempotencyKey,
        String blacklistExceptionId
) {
    /** 不带黑名单例外放行单的构造。 */
    public LeasingIntentRequest(String leadId, String intendedSpaceId, BigDecimal intendedAreaSqm,
            Map<String, Object> rentTerms, String idempotencyKey) {
        this(leadId, intendedSpaceId, intendedAreaSqm, rentTerms, idempotencyKey, null);
    }
}
