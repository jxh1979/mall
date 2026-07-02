package com.complex.platform.analytics;

import java.time.LocalDate;

/** H1 节假日区间手动录入请求(经营参数页)。 */
public record HolidayPeriodRequest(
        String idempotencyKey,
        String holidayName,
        Integer holidayYear,
        LocalDate startDate,
        LocalDate endDate
) {
}
