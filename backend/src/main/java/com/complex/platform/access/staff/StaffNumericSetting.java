package com.complex.platform.access.staff;

// 文件版本：V0.01
/** 未配置时采用出厂值；上下限留空，首次配置必须同时填写。 */
public record StaffNumericSetting(int value, Integer minValue, Integer maxValue) {}
