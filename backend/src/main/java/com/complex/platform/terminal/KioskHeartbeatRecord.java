package com.complex.platform.terminal;

import java.time.Instant;
import java.util.List;

public record KioskHeartbeatRecord(
        String deviceId,
        String status,
        String appVersion,
        List<String> capabilities,
        String lastErrorCode,
        String kioskMode,
        Instant lastHeartbeatAt
) {
}
