package com.complex.platform.facility.dto;

// 文件版本：V0.01

/** 撤销接警值守安排：须写原因。 */
public record FacilityAlarmDutyCancelRequest(
        String reason,
        String idempotencyKey) {}
