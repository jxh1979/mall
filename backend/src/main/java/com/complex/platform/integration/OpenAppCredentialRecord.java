package com.complex.platform.integration;

import java.time.Instant;
import java.util.List;

public record OpenAppCredentialRecord(
        String id,
        String appKey,
        String secretRef,
        String status,
        List<String> allowedEndpointKeys,
        Instant createdAt,
        Instant updatedAt
) {
}
