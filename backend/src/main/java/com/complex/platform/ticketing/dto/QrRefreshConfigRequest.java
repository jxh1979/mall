package com.complex.platform.ticketing.dto;

/** #172(2026-07-19 拍板)：改全场统一动态码刷新间隔请求。refreshIntervalSeconds 须能被 5 整除且 5≤x≤60。 */
public record QrRefreshConfigRequest(
        String idempotencyKey,
        Integer refreshIntervalSeconds
) {
}
