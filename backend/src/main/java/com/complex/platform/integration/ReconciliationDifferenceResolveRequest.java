package com.complex.platform.integration;

import java.util.List;

public record ReconciliationDifferenceResolveRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String operatorUserId,
        String status,
        String resolutionNote
) {
}
