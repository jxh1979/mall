package com.complex.platform.facility.dto;

/** 设备状态变更/告警上报：status 目标态；alarm_source 在置 alarm 时必填(manual|iot)。 */
public record FacilityEquipmentStatusRequest(
        String status,
        String alarmSource,
        String alarmNote,
        String idempotencyKey) {}
