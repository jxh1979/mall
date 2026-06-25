package com.complex.platform.energy.dto;

import java.math.BigDecimal;

/** 能耗抄表录入请求（③ S4）。period/readingValue 必填；readingValue 单调不减（系统据此算用量）。 */
public record EnergyMeterReadingRequest(
        String period,
        BigDecimal readingValue,
        String recordedBy,
        String idempotencyKey
) {
}
