package com.complex.platform.energy.dto;

/** 能耗表建档请求（③ S4）。name/meterType 必填；meterType 在枚举内；merchantId 可空。 */
public record EnergyMeterRequest(
        String name,
        String meterType,
        String spaceId,
        String unit,
        String merchantId,
        String idempotencyKey
) {
}
