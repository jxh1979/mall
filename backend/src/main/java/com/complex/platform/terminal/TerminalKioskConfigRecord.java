package com.complex.platform.terminal;

import java.time.Instant;
import java.util.Map;

public record TerminalKioskConfigRecord(
        String deviceId,
        String kioskMode,
        boolean ticketingEnabled,
        String boundScope,
        Map<String, Object> config,
        Instant updatedAt
) {
}
