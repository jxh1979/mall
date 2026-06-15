package com.complex.platform.bar;

import java.util.List;

public record BarTableMergeRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String mergedSessionId,
        String reason,
        String requestedByUserId,
        Long primarySessionVersion,
        Long mergedSessionVersion
) {
}
