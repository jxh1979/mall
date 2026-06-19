package com.complex.platform.inventory.dto;

import java.util.List;

/**
 * R5 T17 建交接任务：建表头(overall_status=in_progress) + 预置五段 steps(pending, seq 1..5)。
 */
public record TraceHandoverCreateRequest(
        String idempotencyKey,
        String traceCode,
        String itemId,
        String deviceId,
        List<String> evidenceAttachmentIds
) {
}
