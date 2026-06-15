package com.complex.platform.terminal.dto;

import java.util.List;

public record KioskHeartbeatRequest(
        String deviceId,
        String terminalType,
        String status,
        String appVersion,
        List<String> capabilities,
        String lastErrorCode,
        String kioskMode
) {
}
