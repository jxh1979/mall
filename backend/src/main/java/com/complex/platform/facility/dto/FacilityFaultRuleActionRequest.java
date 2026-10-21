package com.complex.platform.facility.dto;

// 文件版本：V0.01

/** 故障处置规则确认或停用（HP-F01 R7）。 */
public record FacilityFaultRuleActionRequest(
        String note,
        String idempotencyKey) {}
