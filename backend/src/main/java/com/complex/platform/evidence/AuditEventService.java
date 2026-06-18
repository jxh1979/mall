package com.complex.platform.evidence;

import com.complex.platform.access.AccessActor;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        String actorId = null;
        String actorType = "staff";
        String authContext = null;
        String merchantId = null;
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                Object principal = auth.getPrincipal();
                if (principal instanceof AccessActor actor) {
                    actorId = actor.id();
                    actorType = actor.accountType() != null ? actor.accountType() : "staff";
                    merchantId = actor.merchantId();
                    authContext = actor.username();
                } else {
                    authContext = auth.getName();
                }
                if (authContext != null && authContext.length() > 64) {
                    authContext = authContext.substring(0, 64);
                }
            }
        } catch (RuntimeException ignored) {
            // best-effort: never suppress the audited operation due to auth extraction failure
        }

        AuditEvent event = new AuditEvent(
                UUID.randomUUID(),
                Instant.now(),
                eventName,
                eventResult,
                actorType,
                actorId,
                authContext,
                "web",
                permissionCode,
                businessType,
                businessId,
                requestId,
                idempotencyKey,
                Map.copyOf(new LinkedHashMap<>(metadata == null ? Map.of() : metadata)),
                merchantId
        );
        return repository.save(event);
    }
}
