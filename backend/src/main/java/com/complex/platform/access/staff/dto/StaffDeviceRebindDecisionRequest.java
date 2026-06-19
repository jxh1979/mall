package com.complex.platform.access.staff.dto;

/** 审批换机生效：批准后新凭证 active、旧凭证 superseded（保证唯一 active）。 */
public record StaffDeviceRebindDecisionRequest(
        String idempotencyKey,
        String operatorNote
) {
}
