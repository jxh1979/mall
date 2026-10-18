package com.complex.platform.amusement.dto;

/** CG-08a 发起全园故障状态：role＝发起人本次所用岗位（amusement_head / engineering_safety）；projectId 缺省取当前项目范围。 */
public record ParkWideFaultInitiateRequest(
        String idempotencyKey,
        String projectId,
        String role,
        String reason
) {
}
