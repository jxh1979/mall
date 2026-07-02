package com.complex.platform.analytics;

/** H1 节假日日历 URL 抓取请求(holiday-cn JSON 格式)。 */
public record HolidayFetchRequest(
        String idempotencyKey,
        String url
) {
}
