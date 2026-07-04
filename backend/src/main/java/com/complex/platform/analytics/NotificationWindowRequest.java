package com.complex.platform.analytics;

/** V219 提醒窗天数 upsert 请求(经营参数页):window_key + days(>0)。 */
public record NotificationWindowRequest(
        String idempotencyKey,
        String windowKey,
        Integer days,
        String description
) {
}
