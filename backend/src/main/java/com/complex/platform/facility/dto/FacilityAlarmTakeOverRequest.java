package com.complex.platform.facility.dto;

// 文件版本：V0.01

/** 设备告警明确接手（「已收到并接手」）。 */
public record FacilityAlarmTakeOverRequest(
        String note,
        String idempotencyKey) {}
