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
        Instant updatedAt,
        // 第 63 条（PGM:623「记录编号、型号、点位、责任人和状态」）：设备型号；安检 X 光设备登记时必填，其他设备为空
        String deviceModel,
        // 第 63 条（BSM:800「仅记设备状态、故障事件与维修工单留痕」）：安检 X 光设备最近一次登记故障时开的维修工单
        String repairWorkOrderId
) {
    /** 不带型号与维修工单的设备（员工端领用绑定的工作手机、平板、一体机）。 */
    public TerminalDeviceRecord(String id, String deviceId, String deviceCode, String terminalType, String bindingScope,
            String boundSpaceId, String mdmDeviceRef, String kioskMode, String merchantId, String venueId, String areaId,
            String locationPointId, String staffId, String status, Instant boundAt, Instant returnedAt, Instant unboundAt,
            String returnReason, String unbindReason, Instant createdAt, Instant updatedAt) {
        this(id, deviceId, deviceCode, terminalType, bindingScope, boundSpaceId, mdmDeviceRef, kioskMode, merchantId, venueId,
                areaId, locationPointId, staffId, status, boundAt, returnedAt, unboundAt, returnReason, unbindReason, createdAt,
                updatedAt, null, null);
    }
}
