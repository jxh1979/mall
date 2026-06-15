package com.complex.platform.terminal;

import java.time.Instant;

public record TerminalCapabilitySwitchRecord(
        String id,
        String terminalType,
        String capabilityCode,
        String scopeType,
        String scopeId,
        boolean enabled,
        String reason,
        Instant createdAt,
        Instant updatedAt
) {
}
