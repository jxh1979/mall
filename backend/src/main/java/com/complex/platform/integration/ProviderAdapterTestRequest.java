package com.complex.platform.integration;

import java.util.List;
import java.util.Map;

public record ProviderAdapterTestRequest(
        String idempotencyKey,
        String providerCode,
        String scenario,
        Map<String, Object> mockPayload,
        String operatorNote,
        List<String> evidenceAttachmentIds
) {
}
