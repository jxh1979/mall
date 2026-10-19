package com.complex.platform.contract;

// 文件版本：V0.01

/**
 * HP-C03 押金收取月数业务参数（BSM L1347「商户押金的收取月数全平台统一（业务参数，全平台统一可配，出厂默认三个月，须同时配置上下限；
 * 下限不得低于三个月，配置校验拒绝更低的值，不允许零押金或负押金）」）。未配置时采用出厂值；上下限留空，首次配置必须同时填写。
 */
public record DepositMonthsSettingRecord(int value, Integer minValue, Integer maxValue) {}
