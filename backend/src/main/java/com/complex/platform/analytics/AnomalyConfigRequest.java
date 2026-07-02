package com.complex.platform.analytics;

import java.math.BigDecimal;

/** M3 异常检测阈值配置请求(经营参数页)。 */
public record AnomalyConfigRequest(
        String idempotencyKey,
        BigDecimal surgePct,
        BigDecimal highPct,
        Boolean enabled
) {
}
