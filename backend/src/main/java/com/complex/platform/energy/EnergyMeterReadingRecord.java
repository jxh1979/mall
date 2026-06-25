package com.complex.platform.energy;

import java.math.BigDecimal;
import java.time.Instant;

/** 能耗抄表读数（energy_meter_readings，V168，③ S4）。consumption=本期 reading - 上期 reading；首读为空。 */
public record EnergyMeterReadingRecord(
        String id,
        String meterId,
        String period,
        BigDecimal readingValue,
        BigDecimal consumption,
        String recordedBy,
        String projectId,
        String merchantId,
        Instant createdAt
) {
}
