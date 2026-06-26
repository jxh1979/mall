package com.complex.platform.energy;

import java.time.Instant;

/** 能源表计台账。4类表计，3类归集对象，支持总分表层级。 */
public record EnergyMeterRecord(
        String id,
        String name,
        String meterType,
        String parentMeterId,
        String gatherObject,
        String spaceId,
        String merchantId,
        String projectId,
        String readMode,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}
