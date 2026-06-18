package com.complex.platform.ticketing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

class TerminalSyncPayloadSanitizerTests {

    // Deterministic stand-in for the real SHA-256 hasher.
    private static final UnaryOperator<String> HASHER = value -> "H" + value.length();

    @Test
    void redactsSensitiveFieldByNameEvenWithoutCredPrefix() {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("entitlement_code", "PLAINCODE123");
        payload.put("credentialPayload", "jwt.header.signature");
        payload.put("pointId", "gate_1");

        Map<String, String> sanitized = TerminalSyncPayloadSanitizer.sanitize(payload, HASHER);

        assertThat(sanitized.get("entitlement_code")).startsWith("sha256:").doesNotContain("PLAINCODE123");
        assertThat(sanitized.get("credentialPayload")).startsWith("sha256:").doesNotContain("jwt.header.signature");
        // Non-sensitive operational fields stay intact for traceability
        assertThat(sanitized.get("pointId")).isEqualTo("gate_1");
    }

    @Test
    void redactsCredPrefixedValueUnderArbitraryKey() {
        Map<String, String> sanitized = TerminalSyncPayloadSanitizer.sanitize(Map.of("misc", "cred_abc123"), HASHER);

        assertThat(sanitized.get("misc")).startsWith("sha256:").doesNotContain("cred_abc123");
    }

    @Test
    void leavesAlreadyHashedAndBlankValuesUnchanged() {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("token", "sha256:deadbeef");
        payload.put("note", "");

        Map<String, String> sanitized = TerminalSyncPayloadSanitizer.sanitize(payload, HASHER);

        assertThat(sanitized.get("token")).isEqualTo("sha256:deadbeef");
        assertThat(sanitized.get("note")).isEmpty();
    }
}
