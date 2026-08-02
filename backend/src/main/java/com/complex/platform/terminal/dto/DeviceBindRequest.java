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
        String kioskMode,
        // K16 一步到位：登记设备时可直接给定所属商户（核销上下文），免运营二次改绑；可空。
        String merchantId
) {
}
