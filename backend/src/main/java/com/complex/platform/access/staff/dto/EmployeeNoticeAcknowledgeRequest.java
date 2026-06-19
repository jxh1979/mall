package com.complex.platform.access.staff.dto;

/**
 * 确认 / 同意通知。consent=true → consented（需同意类通知）；consent=false 或缺省 → acknowledged。
 * 显式 decline 走 declined。
 */
public record EmployeeNoticeAcknowledgeRequest(
        String idempotencyKey,
        String operatorNote,
        Boolean consent,
        Boolean decline
) {
}
