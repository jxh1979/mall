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
        String merchantId,
        // U07-001：领用/绑定即写入持有员工（工作手机必填；平板/一体机按点位或商户绑定可空），不靠归还回填。
        String staffId
) {
    /** 既有 10 参调用点零改动：不带员工（非工作手机场景）。 */
    public DeviceBindRequest(String idempotencyKey, String operatorNote, List<String> evidenceAttachmentIds, String deviceId,
            String terminalType, String bindingScope, String boundSpaceId, String mdmDeviceRef, String kioskMode, String merchantId) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, deviceId, terminalType, bindingScope, boundSpaceId, mdmDeviceRef,
                kioskMode, merchantId, null);
    }
}
