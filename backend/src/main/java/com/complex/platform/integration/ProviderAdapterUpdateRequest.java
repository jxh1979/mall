package com.complex.platform.integration;

import java.util.List;
import java.util.Map;

public record ProviderAdapterUpdateRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String providerCode,
        List<String> capabilities,
        String environment,
        String status,
        Boolean webhookEnabled,
        Boolean pollingCheckEnabled,
        Map<String, Object> fieldMappingPlaceholder,
        String credentialSecretRef
) {
}
