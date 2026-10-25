package com.complex.platform.facility.dto;

// 文件版本：V0.02

/**
 * 设备状态变更/告警上报：status 目标态；alarm_source 在置 alarm 时必填，此入口为员工人工上报只收 manual（OBS-13-06）；
 * priority 在置 alarm/fault 时必填(urgent|high|normal)，由上报人或物联网当场选（HP-F01，2026-10-21 操作员裁决补充第 1 条）。
 */
public record FacilityEquipmentStatusRequest(
        String status,
        String alarmSource,
        String alarmNote,
        String priority,
        String idempotencyKey) {}
