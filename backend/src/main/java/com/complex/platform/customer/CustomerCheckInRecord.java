package com.complex.platform.customer;

import java.time.Instant;
import java.time.LocalDate;

/** 会员签到记录（gap-analysis §7A P2：连续签到阶梯）。每客每日唯一，streak=连续天数。 */
public record CustomerCheckInRecord(
        String id,
        String customerId,
        LocalDate checkInDate,
        int streak,
        long awardedPoints,
        String sourceTerminal,
        Instant createdAt
) {
}
