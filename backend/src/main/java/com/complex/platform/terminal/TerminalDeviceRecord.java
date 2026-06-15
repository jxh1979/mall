package com.complex.platform.terminal;

import java.time.Instant;

public record TerminalDeviceRecord(
        String id,
        String deviceId,
        String deviceCode,
        String terminalType,
        String bindingScope,
        String boundSpaceId,
        String mdmDeviceRef,
        String kioskMode,
        String merchantId,
        String venueId,
        String areaId,
        String locationPointId,
        String staffId,
        String status,
        Instant boundAt,
        Instant returnedAt,
        Instant unboundAt,
        String returnReason,
        String unbindReason,
        Instant createdAt,
        Instant updatedAt
) {
}
