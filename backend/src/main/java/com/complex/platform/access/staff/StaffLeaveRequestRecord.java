package com.complex.platform.access.staff;

import java.time.Instant;
import java.time.LocalDate;

/** F批:请假单(主管审;通过区间考勤月视图记「请假」)。
 * 2026-10-18 操作员裁决：销假=本人申请（revokeRequestedBy/At/RevokeRequestReason，状态 revoke_requested）＋主管批准
 * （revokedBy/At/RevokeReason，状态 revoked）；原审批 reviewedBy/reviewNote 不覆盖。 */
public record StaffLeaveRequestRecord(
        String id,
        String userId,
        String leaveType,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        String status,
        String reviewedBy,
        String reviewNote,
        Instant createdAt,
        Instant updatedAt,
        String revokeRequestedBy,
        Instant revokeRequestedAt,
        String revokeRequestReason,
        String revokedBy,
        Instant revokedAt,
        String revokeReason
) {
    /** 未申请销假、未销假的请假单（销假六列为空）。 */
    public StaffLeaveRequestRecord(String id, String userId, String leaveType, LocalDate startDate, LocalDate endDate,
            String reason, String status, String reviewedBy, String reviewNote, Instant createdAt, Instant updatedAt) {
        this(id, userId, leaveType, startDate, endDate, reason, status, reviewedBy, reviewNote, createdAt, updatedAt,
                null, null, null, null, null, null);
    }
}
