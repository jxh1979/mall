package com.complex.platform.evidence.dto;

import java.util.List;

public record EventBatchRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String deviceCode,
        String staffId,
        List<TerminalEventItemRequest> events
) {
}
