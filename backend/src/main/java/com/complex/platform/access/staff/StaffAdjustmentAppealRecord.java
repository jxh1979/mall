package com.complex.platform.access.staff;

import java.time.Instant;

/**
 * CG-12 员工对已生效扣罚的申诉。status：pending（裁定中）/ upheld（维持）/ revoked（撤销：已冲回，reversalAdjustmentId 为冲回记录）。
 */
public record StaffAdjustmentAppealRecord(
        String id,
        String adjustmentId,
        String userId,
        String month,
        String adjType,
        Long amountCent,
        String reason,
        String reviewId,
        String status,
        String reversalAdjustmentId,
        String initiatedBy,
        Instant createdAt,
        Instant decidedAt
) {
}
