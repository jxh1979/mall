package com.complex.platform.ticketing;

import java.time.Instant;

public record TicketTypeRecord(
        String id,
        String ticketName,
        String baseCalendarPriceMode,
        Long weekdayPriceCent,
        Long weekendPriceCent,
        Long holidayPriceCent,
        boolean stockRequired,
        String status,
        // R5 审计点②：票种有效期天数（可空）。发放时按此天数计算权益 valid_to；
        // 为空则回退默认 1 天（向后兼容）。
        Integer validityDays,
        // P1 清单#19：票种级二维码刷新窗口秒数（可空，取值 {30,60}）。发凭证时按此取值设刷新窗口；
        // 为空则回退默认 60 秒（向后兼容）。
        Integer refreshIntervalSeconds,
        Instant createdAt,
        Instant updatedAt
) {
}
