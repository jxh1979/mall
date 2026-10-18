package com.complex.platform.amusement.dto;

/** CG-08a 发起解除全园故障状态：role＝发起人本次所用岗位；reason＝解除依据（设施恢复情况）。 */
public record ParkWideFaultLiftRequest(
        String idempotencyKey,
        String role,
        String reason
) {
}
