package com.complex.platform.access.staff.dto;

// 文件版本：V0.02
/** U05-011：写口带幂等键，与本控制器其它写口同规。 */
public record StaffNumericSettingRequest(String idempotencyKey, Integer value, Integer minValue, Integer maxValue, String reason) {}
