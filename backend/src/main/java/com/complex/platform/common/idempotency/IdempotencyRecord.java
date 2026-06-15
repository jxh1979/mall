package com.complex.platform.common.idempotency;

import java.time.Instant;
import java.util.UUID;

public record IdempotencyRecord(
        UUID id,
        String operationId,
        String idempotencyKey,
        String requestHash,
        String responseCode,
        String responseBody,
        int httpStatus,
        Instant createdAt,
        Instant expiresAt
) {
}
