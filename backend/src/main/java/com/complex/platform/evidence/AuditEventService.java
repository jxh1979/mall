package com.complex.platform.evidence;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuditEventService {

    private final AuditEventRepository repository;

    public AuditEventService(AuditEventRepository repository) {
        this.repository = repository;
    }

    public AuditEvent recordSuccess(
            String eventName,
            String permissionCode,
            String businessType,
            String businessId,
            String requestId,
            String idempotencyKey,
            Map<String, Object> metadata
    ) {
        return record(eventName, "success", permissionCode, businessType, businessId, requestId, idempotencyKey, metadata);
    }

    public AuditEvent recordFailure(
            String eventName,
            String permissionCode,
            String businessType,
            String businessId,
            String requestId,
            String idempotencyKey,
            String errorCode
    ) {
        return record(eventName, "failure", permissionCode, businessType, businessId, requestId, idempotencyKey, Map.of(
                "error_code", errorCode
        ));
    }

    private AuditEvent record(
            String eventName,
            String eventResult,
            String permissionCode,
            String businessType,
            String businessId,
            String requestId,
            String idempotencyKey,
            Map<String, Object> metadata
    ) {
        AuditEvent event = new AuditEvent(
                UUID.randomUUID(),
                Instant.now(),
                eventName,
                eventResult,
                "staff",
                null,
                "staff-token",
                "web",
                permissionCode,
                businessType,
                businessId,
                requestId,
                idempotencyKey,
                Map.copyOf(new LinkedHashMap<>(metadata == null ? Map.of() : metadata))
        );
        return repository.save(event);
    }
}
