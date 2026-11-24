package com.complex.platform.ticketing;

/**
 * 核销应答。第十八批 CF 段第 409 条 409-R3（BSM:1267「运营容量（软上限）……超过时仅告警提示、仍可放行」）：operatingCapacityReached 为游乐入场放行后
 * 项目在玩人数是否已达运营容量（两种处置方式都提示；未配置运营容量为假）；非游乐核销为空。
 */
public record VerificationResponse(
        String id,
        String verificationNo,
        String status,
        String resultReason,
        Boolean operatingCapacityReached
) {

    public VerificationResponse(String id, String verificationNo, String status, String resultReason) {
        this(id, verificationNo, status, resultReason, null);
    }
}
