package com.complex.platform.finance;

import java.time.Instant;

/**
 * 退款自助额度（refund_authority_limits，V156）：某主体（员工/职级）在某退款域的单笔自助额度（分）。
 * 退款金额 ≤ limitAmountCent 时该主体可自助自动退（无需人工复核）；超过则进复核。
 */
public record RefundAuthorityLimitRecord(
        String id,
        String subjectType,
        String subjectId,
        String refundDomain,
        long limitAmountCent,
        String projectId,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
