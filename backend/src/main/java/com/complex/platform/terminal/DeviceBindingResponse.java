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
        String deviceToken,
        // 第 63 条：设备型号（安检 X 光设备）
        String deviceModel,
        // 第 63 条：安检 X 光设备登记故障时开的维修工单
        String repairWorkOrderId
) {
}
