package com.complex.platform.evidence;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEvent(
        UUID id,
        Instant occurredAt,
        String eventName,
        String eventResult,
        String actorType,
        String actorId,
        String authContext,
        String terminal,
        String permissionCode,
        String businessType,
        String businessId,
        String requestId,
        String idempotencyKey,
        Map<String, Object> metadata
) {
}
