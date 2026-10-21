package com.complex.platform.facility.dto;

// 文件版本：V0.01

/**
 * 接警处置要求记录（HP-F01 R7）：按已生效的故障处置规则处理时填 applied_rule_id；未覆盖时由当班专业处置人员填 judgement
 * （如停用或继续使用）。只记录处置要求，不等于授权平台远程停机，也不改设备状态。
 */
public record FacilityAlarmDispositionRequest(
        String appliedRuleId,
        String judgement,
        String idempotencyKey) {}
