package com.complex.platform.access.staff.dto;

// 文件版本：V0.02

/** 2026-10-18 操作员裁决：员工本人对已批准的请假申请销假（待排班主管批准），须填写理由；主管的批准/驳回走 StaffLeaveReviewRequest。 */
public record StaffLeaveRevokeRequest(
        String idempotencyKey,
        String reason
) {
}
