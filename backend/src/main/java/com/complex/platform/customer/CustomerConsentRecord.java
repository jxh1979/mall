package com.complex.platform.customer;

import java.time.Instant;

public record CustomerConsentRecord(
        String id,
        String customerId,
        String consentType,
        boolean accepted,
        String version,
        String sourceTerminal,
        Instant acceptedAt,
        Instant withdrawnAt,
        Instant createdAt,
        Instant updatedAt
) {
}
