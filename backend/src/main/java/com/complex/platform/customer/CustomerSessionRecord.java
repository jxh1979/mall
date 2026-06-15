package com.complex.platform.customer;

import java.time.Instant;

public record CustomerSessionRecord(
        String id,
        String customerId,
        String customerToken,
        String scene,
        Instant issuedAt,
        Instant expiresAt,
        String status
) {
}
