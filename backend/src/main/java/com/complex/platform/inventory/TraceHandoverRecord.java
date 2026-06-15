package com.complex.platform.inventory;

import java.time.Instant;
import java.util.List;

public record TraceHandoverRecord(
        String id,
        String traceCode,
        String itemId,
        String fromStaffId,
        String toStaffId,
        String deviceId,
        String handoverResult,
        List<String> evidenceAttachmentIds,
        Instant createdAt
) {
}
