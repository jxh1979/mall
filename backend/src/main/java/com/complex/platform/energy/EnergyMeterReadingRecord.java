package com.complex.platform.energy;

import java.math.BigDecimal;
import java.time.Instant;

/** 抄表读数。状态：pending/recorded/reviewed/abnormal；异常只进复核，不拒收。 */
public record EnergyMeterReadingRecord(
        String id,
        String meterId,
        String period,
        BigDecimal readingValue,
        BigDecimal prevReadingValue,
        BigDecimal consumption,
        String status,
        String abnormalReason,
        String readMode,
        String readerId,
        Instant readAt,
        String reviewedBy,
        Instant reviewedAt,
        Instant createdAt,
        Instant updatedAt) {
}
