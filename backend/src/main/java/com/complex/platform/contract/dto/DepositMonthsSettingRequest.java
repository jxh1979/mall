package com.complex.platform.contract.dto;

// 文件版本：V0.01

/** HP-C03 押金收取月数配置（全平台统一）：值与上下限须同时填写，下限不低于 3，值落在上下限闭区间内。 */
public record DepositMonthsSettingRequest(String idempotencyKey, Integer value, Integer minValue, Integer maxValue, String reason) {}
