package com.complex.platform.integration;

import java.util.List;
import java.util.Map;

public record ProviderAdapterRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String providerCode,
        String adapterType,
        String mode,
        Map<String, Object> fieldMapping,
        Map<String, Object> webhookConfig,
        Map<String, Object> pollingConfig,
        Boolean productionReady
) {
}
