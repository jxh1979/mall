package com.complex.platform.entry;

/**
 * 默认实现：能力开关 {@code face.verification.enabled} 关时使用（本期默认）。
 * 不调任何外部人脸/实名厂商，统一返回 {@code RESERVED}，入场不阻塞，年龄由制度兜底
 * （残留风险：人脸关闭期年龄非系统强制，已向用户知会）。
 */
public final class DisabledFaceVerificationGate implements FaceVerificationPort {

    private static final String MSG = "face verification capability is disabled (face.verification.enabled=false); "
            + "entry age enforced by operational policy, not system (residual risk acknowledged)";

    @Override
    public FaceVerificationResult verifyEntry(FaceVerificationRequest request) {
        return FaceVerificationResult.reserved(MSG);
    }
}
