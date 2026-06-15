package com.complex.platform.terminal;

import java.time.Instant;

public record TerminalDeviceStatusEventRecord(
        String id,
        String deviceId,
        String deviceCode,
        String eventType,
        String status,
        String reason,
        String staffId,
        Instant createdAt
) {
}
