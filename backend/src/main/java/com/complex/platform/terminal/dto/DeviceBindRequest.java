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
        String staffId,
        // 第 96 条 96-R1（PGM:623「建设备台账，记录编号、型号、点位、责任人和状态。」）：设备型号；员工端安卓领用工作手机、平板时取本机型号；
        // 可空，不传沿用台账原值。
        String deviceModel
) {
    /** 既有 10 参调用点零改动：不带员工（非工作手机场景）。 */
    public DeviceBindRequest(String idempotencyKey, String operatorNote, List<String> evidenceAttachmentIds, String deviceId,
            String terminalType, String bindingScope, String boundSpaceId, String mdmDeviceRef, String kioskMode, String merchantId) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, deviceId, terminalType, bindingScope, boundSpaceId, mdmDeviceRef,
                kioskMode, merchantId, null, null);
    }

    /** 既有 11 参调用点零改动：不带型号（沿用台账原值）。 */
    public DeviceBindRequest(String idempotencyKey, String operatorNote, List<String> evidenceAttachmentIds, String deviceId,
            String terminalType, String bindingScope, String boundSpaceId, String mdmDeviceRef, String kioskMode, String merchantId,
            String staffId) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, deviceId, terminalType, bindingScope, boundSpaceId, mdmDeviceRef,
                kioskMode, merchantId, staffId, null);
    }
}
