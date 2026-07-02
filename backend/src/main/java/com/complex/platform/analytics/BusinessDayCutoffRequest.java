package com.complex.platform.analytics;

/** S0:经营日切分配置修改请求(域+日切小时)。 */
public record BusinessDayCutoffRequest(
        String idempotencyKey,
        String operatorNote,
        String domain,
        Integer cutoffHour
) {
}
