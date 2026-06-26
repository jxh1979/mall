package com.complex.platform.energy.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** 录入抄表读数。 */
public record EnergyReadingRecordRequest(
        String idempotencyKey,
        String meterId,
        String period,
        BigDecimal readingValue,
        BigDecimal prevReadingValue,
        String readMode,
        String readerId,
        Instant readAt) {
}
