package com.complex.platform.energy.dto;

/** 新建/更新能源表计。 */
public record EnergyMeterRequest(
        String id,
        String name,
        String meterType,
        String parentMeterId,
        String gatherObject,
        String spaceId,
        String merchantId,
        String projectId,
        String readMode,
        Boolean active,
        String idempotencyKey) {
}
