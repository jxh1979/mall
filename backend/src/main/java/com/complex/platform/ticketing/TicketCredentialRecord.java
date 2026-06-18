package com.complex.platform.ticketing;

import java.time.Instant;

public record TicketCredentialRecord(
        String id,
        String entitlementId,
        String orderId,
        String credentialToken,
        String nonce,
        String credentialHash,
        String credentialType,
        Instant issuedAt,
        Instant expiresAt,
        String status,
        int refreshedCount,
        Instant createdAt,
        Instant updatedAt,
        int refreshIntervalSeconds
) {
}
