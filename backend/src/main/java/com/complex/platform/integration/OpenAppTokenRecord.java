package com.complex.platform.integration;

import java.time.Instant;

public record OpenAppTokenRecord(
        String id,
        String appKey,
        String tokenHash,
        Instant issuedAt,
        Instant expiresAt,
        String status,
        String nonce,
        Instant createdAt
) {
}
