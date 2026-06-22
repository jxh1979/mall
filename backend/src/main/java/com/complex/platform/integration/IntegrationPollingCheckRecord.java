package com.complex.platform.integration;

import java.time.Instant;
import java.util.List;

public record IntegrationPollingCheckRecord(
        String id,
        String status,
        String operatorNote,
        List<String> checkedEndpoints,
        int pendingCount,
        int sourceCount,
        List<String> evidenceAttachmentIds,
        Instant checkedAt,
        String productionBoundary
) {
}
