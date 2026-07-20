package com.complex.platform.ticketing;

/** #172(2026-07-19 拍板)：全场统一动态码刷新间隔（秒）响应。 */
public record QrRefreshConfigResponse(int refreshIntervalSeconds) {
}
