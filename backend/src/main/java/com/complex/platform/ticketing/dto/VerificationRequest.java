package com.complex.platform.ticketing.dto;

// 文件版本：V0.02

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.Map;

public record VerificationRequest(
        String idempotencyKey,
        String credentialType,
        @JsonAlias({"entitlement_code", "entitlementCode"})
        String credentialPayload,
        String deviceId,
        String pointId,
        Map<String, Object> businessContext,
        // TK-06：一人多次票入场时检票终端拍下的人脸（base64）；预检 face_binding=capture/match 时必带。
        String faceCapture,
        // TK-06：一人多次票首次留脸前客人单独表示的同意（预检 face_binding=capture 时须为 true，否则不能入场）。
        Boolean faceConsentAccepted
) {
    // ponytail: TK-06 前的 6 参调用点（游乐入场设备核验、既有测试）不带人脸。
    public VerificationRequest(String idempotencyKey, String credentialType, String credentialPayload, String deviceId,
                               String pointId, Map<String, Object> businessContext) {
        this(idempotencyKey, credentialType, credentialPayload, deviceId, pointId, businessContext, null, null);
    }
}
