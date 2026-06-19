package com.complex.platform.inventory;

import java.time.Instant;
import java.util.List;

/**
 * R5 T17：交接任务表头。保留 V012 既有列（含 handover_result 兼容旧单条语义），
 * 新增 overall_status(in_progress/completed/exception) 与 current_segment 承载五段全链状态机。
 */
public record TraceHandoverRecord(
        String id,
        String traceCode,
        String itemId,
        String fromStaffId,
        String toStaffId,
        String deviceId,
        String handoverResult,
        String overallStatus,
        String currentSegment,
        List<String> evidenceAttachmentIds,
        Instant createdAt
) {
}
