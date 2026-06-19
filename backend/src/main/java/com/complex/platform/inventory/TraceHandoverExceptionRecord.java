package com.complex.platform.inventory;

import java.time.Instant;

/**
 * R5 T17 交接异常（缺口/重复/差异）→ 复核。
 * exception_type：gap(机器人送达但无人工接收/超时未接) / duplicate(同段重扫) / diff(机器人与人工不符)。
 * review_status：pending/approved/rejected。
 */
public record TraceHandoverExceptionRecord(
        String id,
        String handoverId,
        String segment,
        String exceptionType,
        String reviewStatus,
        String reviewerUserId,
        String reason,
        Instant createdAt,
        Instant reviewedAt
) {
}
