package com.complex.platform.bar;

import java.util.List;

public record BarTableTransferRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String targetTableId,
        String reason,
        String requestedByUserId,
        Long sessionVersion
) {
}
