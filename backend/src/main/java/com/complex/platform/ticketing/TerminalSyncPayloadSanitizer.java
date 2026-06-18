package com.complex.platform.ticketing;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Redacts sensitive values before a terminal sync payload is persisted/logged. Redaction is driven by the
 * field NAME (not only by a value prefix), so a credential carried under a known sensitive key is hashed even
 * when it does not start with the {@code cred_} marker. The value-prefix check is kept as a fallback for
 * credential markers appearing under arbitrary keys. Non-sensitive operational fields (e.g. pointId/gateId)
 * are left intact for traceability.
 */
final class TerminalSyncPayloadSanitizer {

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "credentialpayload", "credential_payload",
            "entitlementcode", "entitlement_code",
            "credentialtoken", "credential_token",
            "dynamiccredential", "dynamic_credential",
            "credential", "token", "secret", "password");

    private TerminalSyncPayloadSanitizer() {
    }

    static Map<String, String> sanitize(Map<String, String> payload, UnaryOperator<String> hasher) {
        if (payload == null || payload.isEmpty()) {
            return Map.of();
        }
        Map<String, String> sanitized = new LinkedHashMap<>();
        payload.forEach((key, value) -> sanitized.put(key, redact(key, value, hasher)));
        return sanitized;
    }

    private static String redact(String key, String value, UnaryOperator<String> hasher) {
        if (value == null || value.isBlank() || value.startsWith("sha256:")) {
            return value;
        }
        boolean sensitiveField = key != null && SENSITIVE_KEYS.contains(key.toLowerCase(Locale.ROOT));
        if (sensitiveField || value.startsWith("cred_")) {
            return "sha256:" + hasher.apply(value);
        }
        return value;
    }
}
