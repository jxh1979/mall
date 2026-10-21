package com.complex.platform.facility.dto;

// 文件版本：V0.01

/** 告警响应时限矩阵保存一格：alarm_type(elevator|hvac|power|water_fire) × priority(urgent|high|normal)。 */
public record FacilityAlarmPolicyRequest(
        String alarmType,
        String priority,
        Integer responseMinutes,
        Boolean active,
        String idempotencyKey) {}
