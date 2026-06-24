package com.complex.platform.analytics;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 综合体经营参数（complex_operating_params，V160）：坪效面积等综合体（项目）级主数据。
 * operatingAreaSqm 可空=未配置（坪效 revenue.per_sqm 该日诚实空缺，不伪造）。
 */
public record OperatingParamsRecord(
        String projectId,
        BigDecimal operatingAreaSqm,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
