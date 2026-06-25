package com.complex.platform.energy;

import java.time.Instant;

/** 能耗表台账（energy_meters，V168，③ S4）。meterType: electricity|water|gas；merchantId 可空（公区表）。 */
public record EnergyMeterRecord(
        String id,
        String name,
        String meterType,
        String spaceId,
        String unit,
        String status,
        String projectId,
        String merchantId,
        Instant createdAt,
        Instant updatedAt
) {
}
