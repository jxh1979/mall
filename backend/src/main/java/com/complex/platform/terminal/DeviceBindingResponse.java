package com.complex.platform.terminal;

import java.time.Instant;

public record DeviceBindingResponse(
        String id,
        String deviceType,
        String deviceCode,
        String mdmDeviceRef,
        String kioskMode,
        String boundSpaceId,
        String merchantId,
        String venueId,
        String areaId,
        String locationPointId,
        String staffId,
        String status,
        Instant updatedAt,
        String deviceToken
) {
}
