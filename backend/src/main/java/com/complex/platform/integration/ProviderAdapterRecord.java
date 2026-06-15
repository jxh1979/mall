package com.complex.platform.integration;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ProviderAdapterRecord(
        String id,
        String providerCode,
        String adapterType,
        String mode,
        List<String> capabilities,
        String environment,
        String status,
        boolean webhookEnabled,
        boolean pollingCheckEnabled,
        Map<String, Object> fieldMapping,
        Map<String, Object> webhookConfig,
        Map<String, Object> pollingConfig,
        String credentialSecretRef,
        boolean productionReady,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        Instant createdAt,
        Instant updatedAt
) {
}
