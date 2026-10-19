package com.complex.platform.contract.dto;

// 文件版本：V0.01

/** HP-C06 押金退还等待期配置（按项目）：等待日数与上下限须同时填写，下限不小于 0，日数落在上下限闭区间内。 */
public record DepositRefundWaitSettingRequest(String idempotencyKey, String projectId, Integer value, Integer minValue,
        Integer maxValue, String reason) {}
