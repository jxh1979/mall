package com.complex.platform.amusement;

import java.util.List;

/**
 * 游乐设备主数据写入请求（amusement_devices，V143）。
 * deviceCode 必填（项目内唯一，桥接旧 device_id 字符串）；safetyCheckIntervalHours 省略=回退项目级缺省周期；
 * status 省略默认 active；name/locationPointId 可空。operatorNote/evidenceAttachmentIds 供审计留痕。
 */
public record AmusementDeviceRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String projectId,
        String deviceCode,
        String name,
        String status,
        Integer safetyCheckIntervalHours,
        String locationPointId
) {
}
