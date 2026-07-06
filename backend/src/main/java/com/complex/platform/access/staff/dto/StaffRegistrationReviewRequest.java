package com.complex.platform.access.staff.dto;

/**
 * 后台审批员工注册：decision=approve 生成 user_id + 默认角色并发 approval_result 通知；
 * decision=reject 必须带 reject_reason。
 */
public record StaffRegistrationReviewRequest(
        String idempotencyKey,
        String operatorNote,
        String decision,
        String rejectReason,
        String initialPassword
) {
}
