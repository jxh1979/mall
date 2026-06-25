package com.complex.platform.access.dto;

/** 高危授权申请审批请求（④ S2）。decision=approved|rejected；过既有主管复核 SoD/职级/角色闸。 */
public record AccessGrantRequestDecisionRequest(
        String decision,
        String comment,
        String idempotencyKey
) {
}
