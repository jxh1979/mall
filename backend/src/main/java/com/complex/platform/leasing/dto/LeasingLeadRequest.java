package com.complex.platform.leasing.dto;

import java.math.BigDecimal;

/**
 * 招商线索创建请求（V161 重建）。source 4 类、brandId 用于品牌库去重与黑名单拦截。
 * blacklistExceptionId：品牌已拉黑时凭本人已批准的黑名单例外放行单放行（第十八批第 30 条，PGM L502），品牌未被拦时忽略。
 */
public record LeasingLeadRequest(
        String contactName,
        String contactPhone,
        String source,
        String intendedBusinessType,
        String intendedSpaceId,
        BigDecimal intendedAreaSqm,
        String brandId,
        String idempotencyKey,
        String blacklistExceptionId,
        // 18E（第 19 条 19-R2）：落地项目（可选）；不带时有意向铺位的取铺位所在项目，否则取本人项目
        String projectId
) {
    public LeasingLeadRequest(String contactName, String contactPhone, String source, String intendedBusinessType,
            String intendedSpaceId, BigDecimal intendedAreaSqm, String brandId, String idempotencyKey, String blacklistExceptionId) {
        this(contactName, contactPhone, source, intendedBusinessType, intendedSpaceId, intendedAreaSqm, brandId, idempotencyKey,
                blacklistExceptionId, null);
    }

    /** 不带黑名单例外放行单的构造。 */
    public LeasingLeadRequest(String contactName, String contactPhone, String source, String intendedBusinessType,
            String intendedSpaceId, BigDecimal intendedAreaSqm, String brandId, String idempotencyKey) {
        this(contactName, contactPhone, source, intendedBusinessType, intendedSpaceId, intendedAreaSqm, brandId, idempotencyKey,
                null, null);
    }
}
