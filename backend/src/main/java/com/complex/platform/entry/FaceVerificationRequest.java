package com.complex.platform.entry;

/**
 * 人脸入场核验请求（端口入参，厂商无关）。
 *
 * @param credentialPayload 入场凭证载荷（门票动态核销凭证 / 实名身份引用；占位期仅留痕，不解析）
 * @param deviceId          闸机/受理设备 id
 * @param pointId           入场点/售酒点 id
 * @param scenario          场景（entry_checkin / bar_age_inherit 等，仅作留痕/路由提示）
 */
public record FaceVerificationRequest(
        String credentialPayload,
        String deviceId,
        String pointId,
        String scenario
) {
    public static FaceVerificationRequest of(
            String credentialPayload, String deviceId, String pointId, String scenario) {
        return new FaceVerificationRequest(credentialPayload, deviceId, pointId, scenario);
    }
}
