package com.complex.platform.integration;

import java.util.List;

public record IntegrationPollingCheckRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> checkedEndpoints,
        Integer pendingCount,
        Integer sourceCount,
        List<String> evidenceAttachmentIds
) {
}
