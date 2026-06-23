package com.complex.platform.finance;

import java.time.Instant;

/**
 * F-OPS-08 物业费费率（项目级、可按期）。effectivePeriod=null 表示该项目默认费率（精确期未命中时回退）。
 * ratePerSquareMeterCent：每平米单价（分 / ㎡ / 期）。替代已废止的 CAM 权重分摊（cam_allocations）。
 */
public record PropertyFeeRateRecord(
        String id,
        String projectId,
        String effectivePeriod,
        long ratePerSquareMeterCent,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
