package com.complex.platform.finance.dto;

/**
 * F-OPS-08 物业费费率维护（upsert）。effectivePeriod=null/空 表示设为该项目默认费率。
 * ratePerSquareMeterCent：每平米单价（分 / ㎡ / 期，&gt;= 0）。status 缺省 active。
 */
public record PropertyFeeRateRequest(
        String idempotencyKey,
        String projectId,
        String effectivePeriod,
        Long ratePerSquareMeterCent,
        String status
) {
}
