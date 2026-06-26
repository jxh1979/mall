package com.complex.platform.energy;

import java.math.BigDecimal;
import java.time.Instant;

/** 各表计类型异常阈值。surge/drop 为百分比。 */
public record EnergyAnomalyThresholdRecord(
        String id,
        String meterType,
        BigDecimal surgePct,
        BigDecimal dropPct,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}
