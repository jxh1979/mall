package com.complex.platform.terminal.dto;

import java.util.List;

public record DeviceBindRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String deviceId,
        String terminalType,
        String bindingScope,
        String boundSpaceId,
        String mdmDeviceRef,
        String kioskMode
) {
}
