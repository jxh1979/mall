package com.complex.platform.amusement;

import java.util.List;

/**
 * 游乐设备主数据写入请求（amusement_devices，V143）。
 * deviceCode 必填（项目内唯一，桥接旧 device_id 字符串）；safetyCheckIntervalHours 省略=回退项目级缺省周期；
 * status 省略默认 active；name/locationPointId 可空。operatorNote/evidenceAttachmentIds 供审计留痕。
 * 第十八批 CF 段第 407 条 407-R1：safetyClass 必填（critical 或 auxiliary），critical 须带 dependency（dependent 或 independent）；operatorNote 必填，
 * 作首次分级理由、evidenceAttachmentIds 作分级附件，随建档进工程或安全复核；finalApproverId 为该复核的最终审批人（可空）。
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
        String locationPointId,
        String safetyClass,
        String dependency,
        String finalApproverId
) {
}
